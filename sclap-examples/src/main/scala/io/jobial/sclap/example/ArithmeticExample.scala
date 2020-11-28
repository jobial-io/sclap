package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp
import cats.effect.IO
import io.jobial.sclap.implicits._

object ArithmeticExample extends CommandLineApp {

  def add =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(a + b)

  def sub =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(a - b)

  def mul =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(a * b)

  def div =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(a / b)

  def runWithProcessedArgs =
    for {
      addResult <- subcommand("add", add)
      subResult <- subcommand("sub", sub)
      mulResult <- subcommand("mul", mul)
      divResult <- subcommand("div", div)
    } yield for {
      r <- addResult orElse subResult orElse mulResult orElse divResult
    } yield println(r)
}
