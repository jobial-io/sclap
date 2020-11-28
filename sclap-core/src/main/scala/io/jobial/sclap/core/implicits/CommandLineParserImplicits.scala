package io.jobial.sclap.core.implicits

import cats.Show
import cats.effect.{ContextShift, IO}
import io.jobial.sclap.core.{ArgumentValueParser, ArgumentValuePrinterFromShow, CommandLineArgSpecA, NoSpec, OptionArgumentValueParser}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


trait CommandLineParserImplicits {

  final class IOExtraOps[A](val a: IO[A]) {
    def orElse[B >: A](b: IO[B]) =
      a.handleErrorWith(_ => b)
  }

  implicit def ioExtraOps[A](a: IO[A]) =
    new IOExtraOps[A](a)

  implicit def buildCommandLineArgSpec[A](spec: CommandLineArgSpecA[A]) = spec.build

  implicit def commandLineFromIO[A](result: IO[A]) = NoSpec[A](result).build

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  implicit def fromFuture[A](f: => Future[A])(implicit ec: ExecutionContext) =
    IO.fromFuture(IO(f))(IO.contextShift(ec))

  implicit def fromTry[A](t: => Try[A]): IO[A] =
    IO().flatMap(_ => IO.fromTry(t))
}
