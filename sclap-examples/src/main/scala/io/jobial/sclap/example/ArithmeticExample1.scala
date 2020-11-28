package io.jobial.sclap.example

import cats.effect.IO
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.implicits._

object ArithmeticExample1 extends CommandLineApp {

  def operation(op: (Int, Int) => Int) =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(op(a, b))

  def runWithProcessedArgs =
    for {
      addResult <- subcommand("add", operation(_ + _))
      subResult <- subcommand("sub", operation(_ - _))
      mulResult <- subcommand("mul", operation(_ * _))
      divResult <- subcommand("div", operation(_ / _))
    } yield for {
      r <- addResult orElse subResult orElse mulResult orElse divResult
    } yield println(r)
}
