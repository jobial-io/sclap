package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.{ArgumentValueParser, ArgumentValuePrinter}

import java.time.LocalDate
import scala.util.Try

object DateExample extends CommandLineApp {

  implicit val parser = new ArgumentValueParser[LocalDate] {
    def parse(value: String) =
      Try(LocalDate.parse(value)).toEither

    def empty: LocalDate =
      LocalDate.now
  }

  implicit val printer = new ArgumentValuePrinter[LocalDate] {
    def print(value: LocalDate) =
      value.toString
  }

  def run =
    for {
      d <- opt[LocalDate]("date").defaultValue(LocalDate.now).description("The date")
    } yield
      println(s"date: $d")
}
