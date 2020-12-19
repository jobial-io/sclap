package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.example.ArithmeticExample.param


object ArithmeticExample2 extends CommandLineApp {

  def operation(name: String, op: (Int, Int) => Int) =
    subcommand[Int](name) {
      for {
        a <- param[Int].required
        b <- param[Int].required
      } yield op(a, b)
    }

  def run =
    for {
      addResult <- operation("add", _ + _)
      subResult <- operation("sub", _ - _)
      mulResult <- operation("mul", _ * _)
      divResult <- operation("div", _ / _)
    } yield for {
      r <- addResult orElse subResult orElse mulResult orElse divResult
    } yield println(r)

}
