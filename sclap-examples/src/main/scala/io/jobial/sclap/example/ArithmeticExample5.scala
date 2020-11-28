package io.jobial.sclap.example

import cats.effect.IO
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.ArgumentValueParser
import io.jobial.sclap.implicits._

object ArithmeticExample5 extends CommandLineApp {

  def operation[T: ArgumentValueParser](name: String, op: (T, T) => T) =
    subcommand(
      name,
      for {
        a <- param[T].required
        b <- param[T].required
      } yield IO {
        println(a + " " + b)
        op(a, b)
      }
    )

  def runWithProcessedArgs =
    for {
      subcommandResult <- subcommands(
        operation[Double]("add", _ + _),
        operation[Double]("sub", _ - _),
        operation[Double]("mul", _ * _),
        operation[Double]("div", _ / _)
      )
    } yield subcommandResult.map(println)
}
