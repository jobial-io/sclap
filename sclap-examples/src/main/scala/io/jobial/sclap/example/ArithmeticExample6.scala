package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.ArgumentValueParser


object ArithmeticExample6 extends CommandLineApp {

  def operation[T: ArgumentValueParser](name: String, op: (T, T) => T) =
    subcommand[T](name)
      .header(s"${name.toUpperCase} two numbers.")
      .description("Speficy the two operands and the result will be printed.") {
        for {
          a <- param[T].description("The first operand.").required
          b <- param[T].description("The second operand.").required
        } yield op(a, b)
      }

  def run = {
    command("arithmetic")
      .header("Simple arithmetics on the command line.")
      .description("Use the following commands to add, subtract, multiply, divide numbers.") {
        for {
          subcommandResult <- subcommands(
            operation[Double]("add", _ + _),
            operation[Double]("sub", _ - _),
            operation[Double]("mul", _ * _),
            operation[Double]("div", _ / _)
          )
        } yield subcommandResult.map(println)
      }
  }

}
