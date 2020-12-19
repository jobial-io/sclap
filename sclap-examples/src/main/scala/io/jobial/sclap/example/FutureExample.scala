package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object FutureExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("hello")
    } yield Future {
      println(s"hello $hello")
    }
    
}
