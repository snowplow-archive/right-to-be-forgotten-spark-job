/**
  * Copyright 2018 Snowplow Analytics
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.snowplowanalytics.righttobeforgotten

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.databind.node.{ObjectNode, TextNode}
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import org.json4s.jackson.JsonMethods._
import org.apache.spark.sql.functions.udf
import com.snowplowanalytics.snowplow.analytics.scalasdk.json.EventTransformer
import com.snowplowanalytics.iglu.client.repositories.{
  EmbeddedRepositoryRef,
  RepositoryRefConfig
}
import com.snowplowanalytics.iglu.client.{
  Resolver,
  SchemaCriterion,
  SchemaKey,
  ValidatedNel
}
import com.snowplowanalytics.iglu.client.validation.ValidatableJValue
import org.json4s.{JArray, JNothing, JObject, JValue}
import scalaz.{Failure, Success}
import com.jayway.jsonpath.{Configuration, JsonPath, Option => JOption}
import org.json4s.jackson.JsonMethods
import scala.io.Source

object ValueFilters {

  val R2F_DATUM_PREAMBLE =
    """{"schema":"iglu:com.snowplowanalytics.snowplow/r2f_data/jsonschema/1-0-0", "data": """
  val R2F_DATUM_POSTAMBLE = """}"""
  val embeddedSchemaRepo = EmbeddedRepositoryRef(
    RepositoryRefConfig("resources", 1, List("com.snowplowanalytics")),
    "src/main/resources/iglu-client-embedded") // FIXME: add to classpath via sbt
  val resolver = Resolver(cacheSize = 500, embeddedSchemaRepo)

  def createFiltersFromData(
      r2fDataFile: String): Either[List[String], List[ValueFilter]] = {
    val le = Source
      .fromFile(r2fDataFile)
      .getLines
      .toList
      .map(validateSingleR2FLine)
    le.partition(_.isLeft) match {
      case (failures, _) if (failures.nonEmpty) =>
        Left(for (Left(s) <- failures) yield s)
      case (_, filters) => Right(for (Right(f) <- filters) yield f)
    }
  }

  def validateSingleR2FLine(line: String): Either[String, ValueFilter] = {
    val v: ValidatedNel[JValue] =
      ValidatableJValue.validate(
        parse(s"$R2F_DATUM_PREAMBLE $line $R2F_DATUM_POSTAMBLE"),
        true)(resolver)
    v.fold(
      pm => Left(pm.list.map(_.getMessage).mkString("\n")),
      sj => Right(filterFromValidJValue(sj))
    )
  }

  private def filterFromValidJValue(jv: JValue): ValueFilter = {
    implicit val formats = org.json4s.DefaultFormats
    val pjv = jv \ "pojo"
    if (pjv.extractOpt[JObject].nonEmpty) {
      StringValueFilter(
        (pjv \ "fieldName").extract[String],
        Set((pjv \ "valueForWhichRowWillBeDeleted").extract[String]))
    } else {
      val jjv = jv \ "json"
      JSONValueFilter(
        (jjv \ "fieldName").extract[String],
        Set((jjv \ "valueForWhichRowWillBeDeleted").extract[String]),
        (jjv \ "schemaCriterion").extract[String],
        (jjv \ "jsonPath").extract[String]
      )
    }
  }

  def matchR2FDataUDF(filters: List[ValueFilter]) = udf(matchR2FData(filters) _)

  def matchR2FData(filters: List[ValueFilter])(r: Seq[String]): Boolean = {
    EventTransformer
      .getValidatedJsonEvent(r.toArray, false)
      .fold(_ => None, { case (_, jv) => Some(jv) })
      .map(j => filters.exists(_.matches(j)))
      .getOrElse(false)
  }
}

case class StringValueFilter(override val fieldName: String,
                             values: Set[String])
    extends ValueFilter {
  def matches(inputRow: JValue): Boolean = {
    implicit val formats = org.json4s.DefaultFormats
    (inputRow \ fieldName)
      .extractOpt[String]
      .map(values.contains)
      .getOrElse(false)
  }
}

case class JSONValueFilter(override val fieldName: String,
                           values: Set[String],
                           schemaCriterion: String,
                           jsonPath: String)
    extends ValueFilter {

  private val schemaKeyMatcher = getSchemaKeyMatcher(schemaCriterion)

  private def getSchemaKeyMatcher(
      schemaCriterion: String): Function[SchemaKey, Boolean] = {
    SchemaCriterion.parse(schemaCriterion) match {
      case Success(sc) => sc.matches _
      case Failure(msg: ProcessingMessage) =>
        throw new IllegalIgluSchemaStringException(msg.getMessage)
    }
  }

  def matches(inputRow: JValue): Boolean = {
    implicit val formats = org.json4s.DefaultFormats
    val fieldValue = inputRow \ fieldName
    val data = fieldValue \ "data"
    data match {
      case JNothing   => false
      case x: JArray  => x.arr.exists(selfDescribingFieldMatches(_))
      case x: JObject => selfDescribingFieldMatches(x)
      case _          => false
    }
  }

  private def selfDescribingFieldMatches(selfDescEvent: JValue): Boolean =
    schemaMatches(selfDescEvent) && jsonPathMatches(selfDescEvent)

  private def schemaMatches(selfDescEvent: JValue): Boolean = {
    implicit val formats = org.json4s.DefaultFormats
    val schema = (selfDescEvent \ "schema").extractOpt[String]
    schema
      .flatMap(SchemaKey.parse(_).toOption)
      .map(schemaKeyMatcher)
      .getOrElse(false)
  }

  private lazy val jacksonNodeJsonObjectMapper = {
    val objectMapper = new ObjectMapper()
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    objectMapper
  }

  private lazy val jsonPathConf =
    Configuration
      .builder()
      .options(JOption.SUPPRESS_EXCEPTIONS)
      .jsonProvider(
        new JacksonJsonNodeJsonProvider(jacksonNodeJsonObjectMapper))
      .build()

  private def jsonPathMatches(value: JValue): Boolean = {
    val d: JValue = value \ "data"
    val o: ObjectNode = JsonMethods.mapper.valueToTree[ObjectNode](d)
    val documentContext = JsonPath.using(jsonPathConf).parse(o)
    val v = documentContext.read[Object](jsonPath)
    v match {
      case tn: TextNode => values.contains(tn.asText())
      case _            => false
    }
  }
}

sealed trait ValueFilter {
  val fieldName: String
  def matches(inputRows: JValue): Boolean
}
