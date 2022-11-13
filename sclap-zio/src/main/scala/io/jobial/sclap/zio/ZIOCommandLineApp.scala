package io.jobial.sclap.zio

import cats.effect.IO
import cats.effect.std.Dispatcher
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.CommandLine
import zio.{RIO, Runtime}
import zio.interop.catz._
import zio.interop.catz.implicits._

trait ZIOCommandLineApp extends CommandLineApp {

  implicit def env = zio.Runtime.default
  
  implicit def ioFromRIO[R: Runtime, A](fa: RIO[R, A]) = 
    IO.fromFuture(IO(implicitly[Runtime[R]].unsafeRunToFuture[Throwable, A](fa)))  

  implicit def commandLineFromRIO[R: Runtime, A](fa: RIO[R, A]): CommandLine[A] =
    ioFromRIO(fa)
    
}
