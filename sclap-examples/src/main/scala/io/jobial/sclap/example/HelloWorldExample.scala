package io.jobial.sclap.example

import cats.effect.IO
import io.jobial.sclap.CommandLineApp

object HelloWorldExample extends CommandLineApp {

  def run =
    command.header("Hello World")
      .description("A hello world app with one option.") {
        for {
          hello <- opt[String]("hello").defaultValue("world")
        } yield IO {
          println(s"hello $hello")
        }
      }
}