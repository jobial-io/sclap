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
ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.13", "2.13.5")
ThisBuild / version := "1.1.1"

import sbt.Keys.{description, publishConfiguration}
import xerial.sbt.Sonatype._

lazy val commonSettings = Seq(
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  publishTo := publishTo.value.orElse(sonatypePublishToBundle.value),
  sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "orbang@jobial.io")),
  organizationName := "Jobial OÜ",
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  description := "Scala command line apps made simple - a composable and easy-to-use CLI parser built on Cats"
)

lazy val CatsVersion = "2.0.0"
lazy val ScalaLoggingVersion = "3.9.2"
lazy val PicocliVersion = "4.6.1"
lazy val ScalatestVersion = "3.2.3"

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
  )
  .aggregate(`sclap-core`, `sclap-picocli`, `sclap-app`, `sclap-examples`)
  .dependsOn(`sclap-core`, `sclap-picocli`, `sclap-app`)

lazy val `sclap-core` = project
  .in(file("sclap-core"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-effect" % CatsVersion,
      "org.typelevel" %% "cats-free" % CatsVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion
    )
  )

lazy val `sclap-picocli` = project
  .in(file("sclap-picocli"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "info.picocli" % "picocli" % PicocliVersion
    )
  )
  .dependsOn(`sclap-core`)

lazy val `sclap-app` = project
  .in(file("sclap-app"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalatestVersion % "test"
    )
  )
  .dependsOn(`sclap-picocli`)

lazy val `sclap-examples` = project
  .in(file("sclap-examples"))
  .settings(commonSettings)
  .dependsOn(`sclap-app`)
