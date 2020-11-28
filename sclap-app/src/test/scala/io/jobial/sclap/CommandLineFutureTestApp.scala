package io.jobial.sclap

import io.jobial.sclap.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._

object CommandLineFutureTestApp extends CommandLineApp {

  def runWithProcessedArgs =
    command.header("This is an app").description("A really cool app.") {
      for {
        a <- opt[String]("a").paramLabel("<aaa>")
        b <- opt("b", 1)
      } yield Future {
        println((a, b))
      }
    }


}
