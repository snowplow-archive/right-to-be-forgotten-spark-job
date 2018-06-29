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

import sbt._

object Dependencies {

 val resolutionRepos = Seq(
   // For Snowplow libs
    "Snowplow Analytics Maven repo" at "http://maven.snplow.com/releases/",
    "user-agent-parser repo"        at "https://clojars.org/repo/"
 )

  object V {
    // Java
    val jsonpath                          = "2.4.0"
    // Scala
    val spark                             = "2.3.1"
    val commonEnrich                      = "0.33.0"
    val hadoopAWS                         = "2.7.3"
    val snowplowScalaAnalyticsSdkVersion  = "0.3.1"
    val json4sVersion                     = "3.2.11"
    val igluScalaClient                   = "0.5.0"
    val scallop                           = "3.1.2"
    // Scala (test only)
    val scalatest                         = "3.0.5"
  }

  object Libraries {
    // Java
    val jsonPath                   = "com.jayway.jsonpath"   %  "json-path"                     % V.jsonpath
    // Scala
    val sparkCore                  = "org.apache.spark"      %% "spark-core"                    % V.spark                            % "provided"
    val sparkSQL                   = "org.apache.spark"      %% "spark-sql"                     % V.spark                            % "provided"
    val hadoopAWS                  = "org.apache.hadoop"     % "hadoop-aws"                     % V.hadoopAWS                        % "provided"
    val snowplowScalaAnalyticsSdk  = "com.snowplowanalytics" %% "snowplow-scala-analytics-sdk"  % V.snowplowScalaAnalyticsSdkVersion
    val igluScalaClient            = "com.snowplowanalytics" %% "iglu-scala-client"             % V.igluScalaClient
    val json4s                     = "org.json4s"            %% "json4s-jackson"                % V.json4sVersion
    val scallop                    = "org.rogach"            %% "scallop"                       % V.scallop
    // Test
    val scalatest                  = "org.scalatest"         %% "scalatest"                     % V.scalatest                        % "test"
  }
}
