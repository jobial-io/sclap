/*
 * Copyright (c) 2020 Jobial OÜ. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package io.jobial.sclap.core.implicits

import cats.effect.ExitCode
import cats.effect.IO
import io.jobial.sclap.core.CommandLineArgSpecA
import io.jobial.sclap.core.HelpRequested
import io.jobial.sclap.core.NoSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try


trait CommandLineParserImplicits {

  implicit def buildCommandLineArgSpec[A](spec: CommandLineArgSpecA[A]) = spec.build

  implicit def fromTry[A](t: => Try[A]): IO[A] =
    IO().flatMap(_ => IO.fromTry(t))

  implicit def fromEither[A](e: => Either[Throwable, A]): IO[A] =
    IO().flatMap(_ => IO.fromEither(e))

  implicit def fromFuture[A](f: => Future[A])(implicit ec: ExecutionContext) =
    IO.fromFuture(IO(f))(IO.contextShift(ec))

  implicit def commandLineFromIO[A](result: IO[A]) = NoSpec[A](result).build

  implicit def commandLineFromFuture[A](result: => Future[A])(implicit ec: ExecutionContext) = NoSpec[A](result).build

  implicit def commandLineFromTry[A](result: => Try[A]) = NoSpec[A](result).build

  implicit def commandLineFromEither[A](result: => Either[Throwable, A]) = NoSpec[A](result).build

  //TODO: shouldn't we have a commandLineFromAny here?

  final class SubcommandIOExtraOps[A](val a: IO[A]) {
    def orElse[B >: A](b: IO[B]) =
      a.handleErrorWith {
        case t: HelpRequested =>
          IO.raiseError(t)
        case _ =>
          b
      }
  }

  implicit def subcommandIOExtraOps[A](a: IO[A]) =
    new SubcommandIOExtraOps[A](a)
}
