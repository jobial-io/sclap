package io.jobial.sclap.example

import cats.effect.IO
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.{CommandLineArgSpec, SubcommandWithCommandLine}
import io.jobial.sclap.example.ArithmeticExample.param


object ArithmeticExample3 extends CommandLineApp {

  def operation(name: String, op: (Int, Int) => Int) =
    subcommand[Int](name) {
      for {
        a <- param[Int].required
        b <- param[Int].required
      } yield op(a, b)
    }

  def run =
    for {
      subcommandResult <- subcommands(
        operation("add", _ + _),
        operation("sub", _ - _),
        operation("mul", _ * _),
        operation("div", _ / _)
      )
    } yield subcommandResult.map(println)

}
