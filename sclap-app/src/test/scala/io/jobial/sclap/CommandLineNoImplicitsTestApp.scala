package io.jobial.sclap

import cats.effect.IO
import cats.implicits._
import io.jobial.sclap.implicits._

object CommandLineNoImplicitsTestApp extends CommandLineApp {

  def runWithProcessedArgs =
    command.header("This is an app").description("A really cool app.") {
      for {
        a <- opt[String]("a").paramLabel("<aaa>").build
        b <- opt("b", 1).build
      } yield IO {
        println((a, b))
      }
    }


}
