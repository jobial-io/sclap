package io.jobial.sclap

import cats.effect.IO
import cats.implicits._
import io.jobial.sclap.implicits._

object CommandLineTestApp extends CommandLineApp {

  def runWithProcessedArgs =
    command.header("This is an app").description("A really cool app.") {
      for {
        a <- opt[String]("a").paramLabel("<aaa>")
        b <- opt("b", 1)
      } yield IO {
        println((a, b))
      }
    }


}
