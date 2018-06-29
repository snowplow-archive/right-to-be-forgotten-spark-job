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

import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest._

import scala.io.Source
import scala.util.Try
import scala.collection.JavaConverters._

class RightToBeForgottenJobSpec extends fixture.FunSpec {

  val INPUT_DIRECTORY = "src/test/resources/enriched/archive/"
  val R2F_DATA_FILE = "src/test/resources/r2fdata/to_be_forgotten.json"

  override type FixtureParam = Path

  def withFixture(testCode: OneArgTest): Outcome = {
    val td = Files.createTempDirectory("R2FTest")
    info("Using temp dir: " + td)
    val outcomeTry = Try { testCode(td) }
    FileUtils.deleteDirectory(td.toFile)
    outcomeTry.getOrElse(fail)
  }

  describe("Right to be forgotten job") {
    it(
      "Must separate the events that contain the identifiers from those that do not") {
      tempDir: Path =>
        val outputMatchingDataDir = getMatchingOutputDir(tempDir)
        val outputNonMatchingDataDir = getNonMathcingOutputDir(tempDir)
        val conf = getTestSparkConfiguration
        lazy val spark: SparkSession =
          getSparkSession(conf)
        val maximumMatchingFraction = "0.5"
        val arguments = Array[String](
          "--r2f-data-file",
          R2F_DATA_FILE,
          "--input-directory",
          INPUT_DIRECTORY,
          "--matching-output-directory",
          outputMatchingDataDir,
          "--non-matching-output-directory",
          outputNonMatchingDataDir,
          "--maximum-matching-proportion",
          maximumMatchingFraction
        )
        val (matchingData: List[String], nonMatchingData: List[String]) =
          runJobWithArguments(tempDir, arguments, spark)
        val originalInput = Source
          .fromFile(
            INPUT_DIRECTORY + "part-00057-09103e46-8f45-49ba-a1bc-26a869e69633-c000.csv")
          .getLines
          .toList
        val combinedOutput = matchingData ++ nonMatchingData
        assert(originalInput.sorted == combinedOutput.sorted, "No data missing")
        assert(matchingData.size == 3)
        assert(nonMatchingData.size == 6)
        spark.stop()
    }

    it(
      "Must fail execution when more than the maximum proportion of events match") {
      tempDir: Path =>
        val outputMatchingDataDir = getMatchingOutputDir(tempDir)
        val outputNonMatchingDataDir = getNonMathcingOutputDir(tempDir)

        val conf = getTestSparkConfiguration
        lazy val spark: SparkSession =
          getSparkSession(conf)
        val maximumMatchingProportion = "0.1"
        val arguments = Array[String](
          "--r2f-data-file",
          R2F_DATA_FILE,
          "--input-directory",
          INPUT_DIRECTORY,
          "--matching-output-directory",
          outputMatchingDataDir,
          "--non-matching-output-directory",
          outputNonMatchingDataDir,
          "--maximum-matching-proportion",
          maximumMatchingProportion
        )
        val r2fConf =
          ValidatedR2FJobConfiguration(new R2FConfArguments(arguments))
        val r2fJob = new RightToBeForgottenSparkJob(r2fConf)
        assertThrows[MaximumRightToBeForgottenMatchingRecordsViolation](
          r2fJob.run(spark))
        spark.stop()
    }

    it("Must optionally not output matching rows ") { tempDir: Path =>
      val outputNonMatchingDataDir = getNonMathcingOutputDir(tempDir)

      val conf = getTestSparkConfiguration
      lazy val spark: SparkSession =
        getSparkSession(conf)
      val maximumMatchingProportion = "0.34"
      val arguments = Array[String](
        "--r2f-data-file",
        R2F_DATA_FILE,
        "--input-directory",
        INPUT_DIRECTORY,
        "--non-matching-output-directory",
        outputNonMatchingDataDir,
        "--maximum-matching-proportion",
        maximumMatchingProportion
      )
      val (matchingData: List[String], nonMatchingData: List[String]) =
        runJobWithArguments(tempDir, arguments, spark)
      assert(matchingData.size == 0)
      assert(nonMatchingData.size == 6)
      spark.stop()
    }
  }

  private def runJobWithArguments(
      tempDir: Path,
      arguments: Array[String],
      spark: SparkSession): (List[String], List[String]) = {
    val r2fConf =
      ValidatedR2FJobConfiguration(new R2FConfArguments(arguments))
    val r2fJob = new RightToBeForgottenSparkJob(r2fConf)
    r2fJob.run(spark)
    val (matching, nonMatching) = FileUtils
      .listFiles(tempDir.toFile, Array("txt"), true)
      .asScala
      .toList
      .partition(_.getAbsolutePath.contains("matchingOut"))
    val matchingData =
      matching.flatMap(Source.fromFile(_).getLines.toList)
    val nonMatchingData =
      nonMatching.flatMap(Source.fromFile(_).getLines.toList)
    (matchingData, nonMatchingData)
  }

  private def getSparkSession(conf: SparkConf) = {
    SparkSession
      .builder()
      .config(conf)
      .getOrCreate()
  }

  private def getTestSparkConfiguration = {
    new SparkConf()
      .setMaster("local[*]")
      .setAppName("spark-test-app")
      .set("spark.sql.sources.writeJobUUID", "false")
  }

  private def getMatchingOutputDir(tempDir: Path) = {
    tempDir.toAbsolutePath.toString + "/matchingOut"
  }

  private def getNonMathcingOutputDir(tempDir: Path) = {
    tempDir.toAbsolutePath.toString + "/nonMatchingOut"
  }
}
