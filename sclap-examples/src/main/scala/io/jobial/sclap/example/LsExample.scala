package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp

object LsExample extends CommandLineApp {

  def myLs(long: Boolean, dirname: Option[String]) =
    println(s"called ls with $long and $dirname...")

  def run =
    for {
      long <- opt("l", false).aliases("long").description("long format")
      dirname <- param[String].paramLabel("<dir>").description("The directory.")
    } yield
      myLs(long, dirname)

}