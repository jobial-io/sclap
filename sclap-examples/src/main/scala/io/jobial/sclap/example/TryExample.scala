package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp

import scala.util.Try


object TryExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("--hello")
    } yield Try {
      println(s"hello $hello")
    }

}
