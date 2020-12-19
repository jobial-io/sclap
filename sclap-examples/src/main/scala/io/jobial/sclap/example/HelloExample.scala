package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp


object HelloExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("hello")
    } yield
      println(s"hello $hello")
      
}
