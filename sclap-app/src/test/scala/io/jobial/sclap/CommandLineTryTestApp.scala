package io.jobial.sclap

import cats.implicits._
import io.jobial.sclap.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object CommandLineTryTestApp extends CommandLineApp {

  def runWithProcessedArgs =
    command.header("This is an app").description("A really cool app.") {
      for {
        a <- opt[String]("a").paramLabel("<aaa>")
        b <- opt("b", 1)
      } yield Try {
        println((a, b))
      }
    }


}
