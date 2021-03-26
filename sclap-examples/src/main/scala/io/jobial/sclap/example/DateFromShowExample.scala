package io.jobial.sclap.example

import cats.Show
import cats.effect.IO
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.ArgumentValueParser

import java.time.LocalDate
import scala.util.Try

object DateFromShowExample extends CommandLineApp {

  implicit val parser = new ArgumentValueParser[LocalDate] {
    def parse(value: String) =
      Try(LocalDate.parse(value)).toEither

    def empty: LocalDate =
      LocalDate.MIN
  }

  implicit val localDateShow = Show.fromToString[LocalDate]

  def run =
    for {
      d <- opt("date").defaultValue(LocalDate.now).description("The date")
    } yield IO {
      println(s"date: $d")
    }
}
