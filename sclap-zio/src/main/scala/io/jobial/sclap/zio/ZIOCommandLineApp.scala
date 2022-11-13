package io.jobial.sclap.zio

import cats.effect.IO
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.CommandLine
import zio.{RIO, Runtime}

trait ZIOCommandLineApp extends CommandLineApp {

  implicit def zioRuntime = zio.Runtime.default

  // TODO: I'm sure there's a better way to do this, until then...
  implicit def ioFromRIO[R: Runtime, A](fa: RIO[R, A]) =
    IO.fromFuture(IO(implicitly[Runtime[R]].unsafeRunToFuture[Throwable, A](fa)))

  implicit def commandLineFromRIO[R: Runtime, A](fa: RIO[R, A]): CommandLine[A] =
    ioFromRIO(fa)
}
