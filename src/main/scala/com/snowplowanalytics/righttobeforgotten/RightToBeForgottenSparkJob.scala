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

import org.apache.spark.sql.{Column, DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.functions.{col, not, split}

class RightToBeForgottenSparkJob(r2fConf: ValidatedR2FJobConfiguration) {
  val inputDirectory = r2fConf.inputDirectory
  val outputMatchingDirectory = r2fConf.outputMatchingDataDirectory
  val outputNonMatchingDirectory = r2fConf.outputNonMatchingDataDirectory
  val RAW_ENRICHED_EVENT_COLUMN = "value"
  val FILTER_MATCHES_COLUMN = "matchesR2FData"
  val SPLIT_ENRICHED_EVENT_COLUMN = "enrichedEvent"

  def run(spark: SparkSession): Unit = {
    val input = spark.read.textFile(inputDirectory)
    val filterColumnDf: DataFrame = getFilterColumnDF(input)
    val filterColumn = col(FILTER_MATCHES_COLUMN)
    val matchingRows =
      getFilteredDataframe(filterColumnDf, filterColumn)
    val nonMatchingRows =
      getFilteredDataframe(filterColumnDf, not(filterColumn))
    val matchingRowsCount = matchingRows.rdd.count().toDouble
    val nonMatchingRowsCount = nonMatchingRows.rdd.count().toDouble
    val prop = matchingRowsCount / (matchingRowsCount + nonMatchingRowsCount)
    if (prop >= r2fConf.maximumMatchingProportion)
      throw new MaximumRightToBeForgottenMatchingRecordsViolation(
        s"Maximum matching records proportion was set to ${r2fConf.maximumMatchingProportion} but $prop of all records actually matched")
    writeOutput(matchingRows, outputMatchingDirectory)
    writeOutput(nonMatchingRows, Some(outputNonMatchingDirectory))
  }

  private def writeOutput(matchingRows: DataFrame,
                          outputDirectory: Option[String]): Unit =
    outputDirectory match {
      case Some(oD) => matchingRows.write.text(oD)
      case None     => {}
    }

  private def getFilteredDataframe(filteredDf: DataFrame,
                                   whereClause: Column): DataFrame =
    filteredDf
      .select(RAW_ENRICHED_EVENT_COLUMN)
      .where(whereClause)
      .persist()

  private def getFilterColumnDF(input: Dataset[String]): DataFrame = {
    val vF = r2fConf.valueFilters
    val enrichedEventsDf =
      input.withColumn(SPLIT_ENRICHED_EVENT_COLUMN,
                       split(col(RAW_ENRICHED_EVENT_COLUMN), "\t"))
    enrichedEventsDf
      .withColumn(
        FILTER_MATCHES_COLUMN,
        ValueFilters.matchR2FDataUDF(vF)(col(SPLIT_ENRICHED_EVENT_COLUMN)))
  }
}
