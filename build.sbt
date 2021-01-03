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
ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.12", "2.13.3")
ThisBuild / version := "0.9.0"

import sbt.Keys.{description, publishConfiguration}
import xerial.sbt.Sonatype._

ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "orbang@jobial.io"))
ThisBuild / publishTo := sonatypePublishTo.value
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
publishTo := sonatypePublishToBundle.value
sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "orbang@jobial.io"))
organizationName := "Jobial OÜ"
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

val CatsVersion = "2.0.0"

lazy val root: Project = project
  .in(file("."))
  .aggregate(`sclap-core`, `sclap-picocli`, `sclap-app`, `sclap-examples`)
  .dependsOn(`sclap-core`, `sclap-picocli`, `sclap-app`)

lazy val `sclap-core` = project
  .in(file("sclap-core"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-effect" % CatsVersion,
      "org.typelevel" %% "cats-free" % CatsVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    ),
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishTo := sonatypePublishToBundle.value,
    sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "orbang@jobial.io")),
    organizationName := "Jobial OÜ",
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    sources in (Compile,doc) := Seq.empty
  )

lazy val `sclap-picocli` = project
  .in(file("sclap-picocli"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-effect" % CatsVersion,
      "org.typelevel" %% "cats-free" % CatsVersion,
      "info.picocli" % "picocli" % "4.5.2"
    ),
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishTo := sonatypePublishToBundle.value,
    sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "Jobial OÜ", "orbang@jobial.io")),
    organizationName := "Jobial OÜ",
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
  )
  .dependsOn(`sclap-core`)

lazy val `sclap-app` = project
  .in(file("sclap-app"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.3" % "test"
    ),
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishTo := sonatypePublishToBundle.value,
    sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "orbang@jobial.io")),
    organizationName := "Jobial OÜ",
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
  )
  .dependsOn(`sclap-picocli`)

lazy val `sclap-examples` = project
  .in(file("sclap-examples"))
  .dependsOn(`sclap-app`)
  .settings(
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    publishTo := sonatypePublishToBundle.value,
    sonatypeProjectHosting := Some(GitHubHosting("jobial-io", "sclap", "orbang@jobial.io")),
    organizationName := "Jobial OÜ",
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
  )
