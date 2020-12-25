/*
 * Copyright (c) 2020 Jobial OÜ.
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

name := "sclap"

ThisBuild / organization := "io.jobial"
ThisBuild / crossScalaVersions := Seq("3.0.0-M2", "2.11.12", "2.12.12", "2.13.3")
ThisBuild / version := "0.9.0"

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
    )
  )

lazy val `sclap-picocli` = project
  .in(file("sclap-picocli"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-effect" % CatsVersion,
      "org.typelevel" %% "cats-free" % CatsVersion,
      "info.picocli" % "picocli" % "4.5.2"
    )
  )
  .dependsOn(`sclap-core`)

lazy val `sclap-app` = project
  .in(file("sclap-app"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.3" % "test"
    )
  )
  .dependsOn(`sclap-picocli`)

lazy val `sclap-examples` = project
  .in(file("sclap-examples"))
  .dependsOn(`sclap-app`)
