package io.jobial.sclap.example

import cats.effect.IO
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.implicits._

object ArithmeticExample3 extends CommandLineApp {

  def operation(name: String, op: (Int, Int) => Int) =
    subcommand(
      name,
      for {
        a <- opt[Int]("a").required
        b <- opt[Int]("b").required
      } yield IO(op(a, b))
    )

  def runWithProcessedArgs =
    for {
      subcommandResult <- subcommands(
        operation("add", _ + _),
        operation("sub", _ - _),
        operation("mul", _ * _),
        operation("div", _ / _)
      )
    } yield subcommandResult.map(println)
}
