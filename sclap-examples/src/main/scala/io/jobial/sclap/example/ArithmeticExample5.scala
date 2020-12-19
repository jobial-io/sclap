package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.ArgumentValueParser


object ArithmeticExample5 extends CommandLineApp {

  def operation[T: ArgumentValueParser](name: String, op: (T, T) => T) =
    subcommand[T](
      name,
      for {
        a <- param[T].required
        b <- param[T].required
      } yield op(a, b)
    )

  def run =
    for {
      subcommandResult <- subcommands(
        operation[Double]("add", _ + _),
        operation[Double]("sub", _ - _),
        operation[Double]("mul", _ * _),
        operation[Double]("div", _ / _)
      )
    } yield subcommandResult.map(println)

}
