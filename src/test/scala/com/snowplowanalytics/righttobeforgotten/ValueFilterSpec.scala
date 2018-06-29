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

import java.io.File

import scala.io.Source
import org.scalatest._
import org.scalatest.Matchers._
import com.snowplowanalytics.snowplow.analytics.scalasdk.json.EventTransformer

class ValueFilterSpec extends FunSpec {

  val stringFilter = new StringValueFilter(
    "user_ipaddress",
    Set(
      "3dc7f1845f237bf3729a36d82ecb0d3eb470ecad4f70b84a778d70a8a2ce4220cb6cc1d916d6843a0f1fa6651345b65a4a542257736021c36f7f51a15e8eb834e")
  )
  val jsonFilter = new JSONValueFilter("unstruct_event",
                                       Set("alice@example.com"),
                                       "iglu:com.mailgun/recipient_unsubscribed/jsonschema/1-0-*",
                                       "$.recipient")

  val r2fRemovalCriteriaFileLines =
    scala.io.Source.fromFile(ValueFilterSpec.R2fRemovalCriteriaRelativePath).getLines.toList

  describe("ValueFilters") {
    val lengthOfCriteriaFile: Long = 3
    it("Read in a file using a URL") {
      val lines = ValueFilters.getRemovalCriteriaLines(ValueFilterSpec.R2fRemovalCriteria)
      lines should have size (lengthOfCriteriaFile)
    }
    it("Validate individual r2f POJO data lines against the schema producing string filters") {
      val firstLine =
        r2fRemovalCriteriaFileLines.headOption
      ValueFilters.validateSingleCriterion(firstLine.getOrElse("No first line")) should be(Right(stringFilter))
    }

    it("Validate individual r2f POJO data lines against the schema producing json filters") {
      val secondLine =
        r2fRemovalCriteriaFileLines(1)
      ValueFilters.validateSingleCriterion(secondLine) should be(Right(jsonFilter))
    }

    it("Must create filters from data files") {
      val filters =
        ValueFilters
          .createFiltersFromRemovalCriteria(r2fRemovalCriteriaFileLines)
          .right
          .getOrElse(List.empty[ValueFilter])
      filters should have size (lengthOfCriteriaFile)
      filters should contain(stringFilter)
      filters should contain(jsonFilter)
    }

    val input = Source
      .fromFile(ValueFilterSpec.InputFile)
      .getLines
      .toList

    val stringSeqInput = for {
      line <- input
    } yield line.split("\t", -1)

    val json = for {
      row <- stringSeqInput
      validated <- EventTransformer
        .getValidatedJsonEvent(row, false)
        .fold(err => { println(err); None }, { case (_, jv) => Some(jv) })
    } yield validated

    it("String filter must match correct rows") {
      val (matching, nonMatching) =
        json.partition(stringFilter.matches(_))
      assert(nonMatching.size == 8)
      assert(matching.size == 1)
      assert(matching.headOption.getOrElse("No first line") == json(3))
    }

    it("JSON filter must match correct rows") {
      val (matching, nonMatching) =
        json.partition(jsonFilter.matches(_))
      assert(nonMatching.size == 8)
      assert(matching.size == 1)
      assert(matching.headOption.getOrElse("No first line") == json(5))
    }

    it("List of filters should match correct lines for String and JSON") {
      val filters =
        ValueFilters
          .createFiltersFromRemovalCriteria(r2fRemovalCriteriaFileLines)
          .right
          .getOrElse(List.empty[ValueFilter])
      val udfFilter = ValueFilters.filtersMatchEvent(filters) _
      val filtered  = stringSeqInput.map(udfFilter(_))
      assert(filtered.size == 9)
      assert(
        filtered == List(false, false, false, true, // string filter case
          false, true, // unstruct_event case
          true, // contexts case
          false, false))
    }
  }
}

object ValueFilterSpec {
  val R2fRemovalCriteriaRelativePath = "src/test/resources/r2fdata/removal_criteria.json"
  lazy val R2fRemovalCriteria = {
    val pwd = new File(".").getAbsoluteFile.getParent
    "file://" + pwd + "/" + R2fRemovalCriteriaRelativePath
  }
  val InputFile =
    "src/test/resources/enriched/archive/part-00057-09103e46-8f45-49ba-a1bc-26a869e69633-c000.csv"
}
