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

// SBT
import sbt._
import Keys._

// Sbt buildinfo plugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

// Assembly
import sbtassembly.AssemblyPlugin.autoImport._

// Scalafmt plugin
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._

object BuildSettings {

  // Basic settings for our app
  lazy val basicSettings = Seq(
    organization  := "com.snowplowanalytics",
    scalaVersion  := "2.11.12",
    scalacOptions := compilerOptions,
    scalacOptions in (Compile, console) --= Seq("-Ywarn-unused-import", "-Xfatal-warnings"),
    javacOptions  := javaCompilerOptions,
    parallelExecution in Test := false, // Parallel tests cause havoc with Spark
    resolvers     ++= Dependencies.resolutionRepos
  )

  lazy val compilerOptions = Seq(
    "-deprecation",                     // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",               // Specify character encoding used by source files.
    "-explaintypes",                    // Explain type errors in more detail.
    "-feature",                         // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",           // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",    // Allow macro definition (besides implementation and application)
    "-language:higherKinds",            // Allow higher-kinded types
    "-language:implicitConversions",    // Allow definition of implicit functions called views
    "-unchecked",                       // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                      // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                 // Fail the compilation if there are any warnings.
    "-Xfuture",                         // Turn on future language features.
    "-Xlint:adapted-args",              // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:delayedinit-select",        // Selecting member of DelayedInit.
    "-Xlint:doc-detached",              // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",              // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                 // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",      // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",              // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",           // Option.apply used implicit view.
    "-Xlint:package-object-classes",    // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",    // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",            // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",               // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",     // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",             // Pattern match may not be typesafe.
    "-Yno-adapted-args",                // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",            // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                 // Warn when dead code is identified.
    "-Ywarn-inaccessible",              // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                 // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",              // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",             // Warn when numerics are widened.
    "-Ywarn-unused-import",             // Warn if an import selector is not referenced.
    "-Ywarn-value-discard"              // Warn when non-Unit expression results are unused.
  )

  lazy val javaCompilerOptions = Seq(
    "-source", "1.8",
    "-target", "1.8"
  )

  lazy val buildInfo = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.snowplowanalytics.righttobeforgotten")

  lazy val buildSettings = basicSettings ++ buildInfo

  lazy val sbtAssemblySettings = Seq(
    // Slightly cleaner jar name
    assemblyJarName in assembly := { name.value + "-" + version.value + ".jar" },
    assemblyMergeStrategy in assembly := {
      case x if x.startsWith("META-INF") => MergeStrategy.discard
      case x if x.endsWith(".html") => MergeStrategy.discard
      case x if x.endsWith("package-info.class") => MergeStrategy.first
      case PathList("com", "google", "common", tail@_*) => MergeStrategy.first
      case PathList("org", "apache", "spark", "unused", tail@_*) => MergeStrategy.first
      case "build.properties" => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
  
  lazy val formatting = Seq(
    scalafmtConfig    := file(".scalafmt.conf"),
    scalafmtOnCompile := true,
    scalafmtVersion   := "1.3.0"
  )

  lazy val dependencies = Seq(
    libraryDependencies ++= Seq(
      Dependencies.Libraries.sparkCore,
      Dependencies.Libraries.sparkSQL,
      Dependencies.Libraries.snowplowScalaAnalyticsSdk,
      Dependencies.Libraries.igluScalaClient,
      Dependencies.Libraries.json4s,
      Dependencies.Libraries.jsonPath,
      Dependencies.Libraries.scallop,
      // // Test
      Dependencies.Libraries.scalatest
  ))
}
