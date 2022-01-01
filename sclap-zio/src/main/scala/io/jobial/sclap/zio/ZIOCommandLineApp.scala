package io.jobial.sclap.zio

import cats.effect.{ConcurrentEffect, IO}
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.CommandLine
import zio.{RIO, Runtime}
import zio.interop.catz._
import zio.interop.catz.taskEffectInstance
import zio.interop.catz.implicits._

trait ZIOCommandLineApp extends CommandLineApp {

  implicit def runtime = zio.Runtime.default
  
  implicit def ioFromRIO[R: Runtime, A](fa: RIO[R, A]) = ConcurrentEffect[RIO[R, *]].toIO(fa)

  implicit def commandLineFromRIO[R: Runtime, A](fa: RIO[R, A]): CommandLine[A] =
    ioFromRIO(fa)
    
}
