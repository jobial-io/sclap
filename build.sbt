/*
 * Copyright (c) 2020 Jobial OÜ. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

name := "sclap"

ThisBuild / organization := "io.jobial"
ThisBuild / crossScalaVersions := Seq("2.11.12")
ThisBuild / version := "1.3.5"
ThisBuild / publishArtifact in (Test, packageBin) := true
ThisBuild / publishArtifact in (Test, packageSrc) := true
ThisBuild / publishArtifact in (Test, packageDoc) := true

import sbt.Keys.{description, publishConfiguration}
import xerial.sbt.Sonatype._

lazy val commonSettings = Seq(
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  publishTo := publishTo.value.orElse(sonatypePublishToBundle.value),
  sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "orbang@jobial.io")),
  organizationName := "Jobial OÜ",
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  description := "Scala command line apps made simple - a composable and easy-to-use CLI parser built on Cats",
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
  scalacOptions ++= (if (scalaBinaryVersion.value != "2.13") Seq("-Ypartial-unification") else Seq())
)

lazy val CatsVersion = "2.0.0"
lazy val CatsEffectVersion = "2.0.0"
lazy val ScalaLoggingVersion = "3.9.2"
lazy val PicocliVersion = "4.6.1"
lazy val ScalatestVersion = "3.2.3"
lazy val ZioVersion = "2.0.0.0-RC13" // TODO: upgrade when Cats version is upgraded

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
  )
  .aggregate(`sclap-core`, `sclap-picocli`, `sclap-app`, `sclap-examples`, `sclap-zio`)
  .dependsOn(`sclap-core`, `sclap-picocli`, `sclap-app` % "compile->compile;test->test")

lazy val `sclap-core` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-free" % CatsVersion,
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion
    )
  )

lazy val `sclap-picocli` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "info.picocli" % "picocli" % PicocliVersion
    )
  )
  .dependsOn(`sclap-core`)

lazy val `sclap-app` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalatestVersion % Test
    )
  )
  .dependsOn(`sclap-picocli`)

lazy val `sclap-examples` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalatestVersion % Test
    )
  )
  .dependsOn(`sclap-app` % "compile->compile;test->test", `sclap-zio`)

lazy val `sclap-zio` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-interop-cats" % ZioVersion
    )
  )
  .dependsOn(`sclap-app`)
