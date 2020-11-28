package io.jobial.sclap.example

import cats.effect.IO
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.implicits._

object ArithmeticExample2 extends CommandLineApp {

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
      addResult <- operation("add", _ + _)
      subResult <- operation("sub", _ - _)
      mulResult <- operation("mul", _ * _)
      divResult <- operation("div", _ / _)
    } yield for {
      r <- addResult orElse subResult orElse mulResult orElse divResult
    } yield println(r)
}
