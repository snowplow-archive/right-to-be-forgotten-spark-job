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
  val inputDirectory             = r2fConf.inputDirectory
  val outputMatchingDirectory    = r2fConf.outputMatchingDataDirectory
  val outputNonMatchingDirectory = r2fConf.outputNonMatchingDataDirectory

  /**
   * Runs the spark job
   * @param sparkSession
   */
  def run(sparkSession: SparkSession): Unit = {
    val input                     = sparkSession.read.textFile(inputDirectory)
    val filterColumnDf: DataFrame = getFilterColumnDF(input)
    val filterColumn              = col(RightToBeForgottenSparkJob.FilterMatchedColumn)
    val matchingRows =
      getFilteredDataFrame(filterColumnDf, filterColumn)
    val nonMatchingRows =
      getFilteredDataFrame(filterColumnDf, not(filterColumn))
    val matchingRowsCount    = matchingRows.count().toDouble
    val nonMatchingRowsCount = nonMatchingRows.count().toDouble
    val prop                 = matchingRowsCount / (matchingRowsCount + nonMatchingRowsCount)
    if (prop >= r2fConf.maximumMatchingProportion)
      throw new MaximumRightToBeForgottenMatchingRecordsViolation(
        s"Maximum matching records proportion was set to ${r2fConf.maximumMatchingProportion} but $prop of all records actually matched")
    writeOutput(matchingRows, outputMatchingDirectory)
    writeOutput(nonMatchingRows, Some(outputNonMatchingDirectory))
  }

  private def writeOutput(matchingRows: DataFrame, outputDirectory: Option[String]): Unit =
    outputDirectory match {
      case Some(oD) => matchingRows.write.text(oD)
      case None     => {}
    }

  private def getFilteredDataFrame(filteredDf: DataFrame, whereClause: Column): DataFrame =
    filteredDf
      .select(RightToBeForgottenSparkJob.RawEnrichedEventColumn)
      .where(whereClause)
      .persist()

  private def getFilterColumnDF(input: Dataset[String]): DataFrame = {
    val vF = r2fConf.valueFilters
    val enrichedEventsDf =
      input.withColumn(RightToBeForgottenSparkJob.SplitEnrichedEventColumn,
                       split(col(RightToBeForgottenSparkJob.RawEnrichedEventColumn), "\t"))
    enrichedEventsDf
      .withColumn(RightToBeForgottenSparkJob.FilterMatchedColumn,
                  ValueFilters.filtersMatchEventUDF(vF)(col(RightToBeForgottenSparkJob.SplitEnrichedEventColumn)))
  }
}

object RightToBeForgottenSparkJob {
  val RawEnrichedEventColumn   = "value"
  val FilterMatchedColumn      = "matchesR2FData"
  val SplitEnrichedEventColumn = "enrichedEvent"
}
