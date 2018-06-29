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
package com.snowplowanalytics
package righttobeforgotten

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.rogach.scallop.ScallopConf

object Main {

  def main(args: Array[String]): Unit = {
    val appArgsValidated = new R2FConfArguments(args)
    val r2fConfig = ValidatedR2FJobConfiguration(appArgsValidated)
    val sparkConf = new SparkConf()
      .setAppName("snowplow-r2f-job")
    val spark: SparkSession =
      SparkSession
        .builder()
        .config(sparkConf)
        .getOrCreate()
    val job = new RightToBeForgottenSparkJob(r2fConfig)
    job.run(spark)
  }
}

class R2FConfArguments(arguments: Seq[String]) extends ScallopConf(arguments) {
  val r2fdata = opt[String](name = "r2f-data-file", required = true)
  val inputDirectory = opt[String](name = "input-directory", required = true)
  val matchingOutputDirectory =
    opt[String](name = "matching-output-directory", required = false)
  val nonMatchingOuputDirectory =
    opt[String](name = "non-matching-output-directory", required = true)
  val maximumMatchingProportion =
    opt[Double](name = "maximum-matching-proportion", required = true)
  verify()
}

case class ValidatedR2FJobConfiguration(arguments: R2FConfArguments) {
  val inputDirectory = arguments.inputDirectory()
  val r2fDataFile = arguments.r2fdata()
  val outputMatchingDataDirectory = arguments.matchingOutputDirectory.toOption
  val outputNonMatchingDataDirectory = arguments.nonMatchingOuputDirectory()
  val maximumMatchingProportion = arguments.maximumMatchingProportion()
  val valueFilters = ValueFilters
    .createFiltersFromData(r2fDataFile)
    .fold(errorList =>
            throw new InvalidDataInRightToBeForgottenConfiguration(
              errorList.mkString("\n")),
          vF => vF)
}
