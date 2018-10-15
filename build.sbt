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
lazy val root = project
  .in(file("."))
  .settings(
    organizationName := "Snowplow Analytics Ltd.",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    name := "snowplow-right-to-be-forgotten-job",
    version := "0.1.0-rc3",
    description := "The Snowplow right to be forgotten job",
    fork := true // For spark
  )
  .settings(BuildSettings.formatting)
  .settings(BuildSettings.buildSettings)
  .settings(BuildSettings.sbtAssemblySettings)
  .settings(BuildSettings.dependencies)

shellPrompt := { _ =>
  "r2f> "
}
