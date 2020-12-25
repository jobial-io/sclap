package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp

object TarExample extends CommandLineApp {

  def tar(create: Option[Boolean], verbose: Option[Boolean], file: Option[String]) =
    println(s"tar create $create verbose $verbose file $file")

  def run =
    command.clusteredShortOptionsAllowed(false) {
      for {
        create <- opt[Boolean]("c")
        verbose <- opt[Boolean]("v")
        file <- opt[String]("f").alias("file")
      } yield
        tar(create, verbose, file)
    }

}