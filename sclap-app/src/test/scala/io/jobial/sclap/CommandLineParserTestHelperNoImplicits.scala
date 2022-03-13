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
package io.jobial.sclap

import cats.effect.IO
import cats.implicits._
import io.jobial.sclap.core.{CommandLine, CommandLineArgSpec, CommandLineParsingFailed, CommandLineParsingFailedForSubcommand, UsageHelpRequested, VersionHelpRequested}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.{Assertion, Succeeded}

import java.io.{ByteArrayOutputStream, PrintStream}
import scala.reflect.ClassTag
import scala.util.{DynamicVariable, Failure, Success, Try}

case class TestCase[T](args: Seq[String], check: TestCheck[T])

sealed trait TestCheck[T] {
  def assertion: TestResult[T] => IO[Assertion]

  def behave: String
}

case class TestSuccessCheck[T](assertion: TestResult[T] => IO[Assertion]) extends TestCheck[T] {

  def behave = "succeed"
}

case class TestFailureCheck[T](assertion: TestResult[T] => IO[Assertion]) extends TestCheck[T] {

  def behave = "fail"
}

case class TestResult[T](result: Either[Throwable, T], out: String, err: String)

trait CommandLineParserTestHelperNoImplicits extends CommandLineParserNoImplicits with OutputCaptureUtils {
  this: AsyncFlatSpec =>

  def runCommandLineTestCases[A](spec: CommandLineArgSpec[IO[A]])(testCases: (Seq[String], TestCheck[A])*) =
    for {
      (args, check) <- testCases
    } yield
      it should s"${check.behave} for args: ${args.mkString(" ")}" in {
        val checkResult =
          for {
            result <- captureOutput {
              executeCommandLine(spec, args.toList)
                .redeem(Left[Throwable, A](_), Right(_)).unsafeRunSync
            }
            r <- check.assertion(TestResult(result.result.flatMap(x => x), result.out, result.err))
          } yield r
        checkResult.unsafeToFuture
      }

  def runCommandLineTestCases(app: CommandLineAppNoImplicits)(testCases: (Seq[String], TestCheck[Any])*): Any =
    runCommandLineTestCases[Any](app.run)(testCases: _*)
  
  def succeedWith[T](result: T, out: Option[String] = None, err: Option[String] = None) =
    TestSuccessCheck({ testResult: TestResult[T] =>
      testResult.result match {
        case Right(r) =>
          IO(logger.debug(r.toString)) *>
            IO(out.map(out => assert(convertToEqualizer(out) === testResult.out))) *>
            IO(err.map(err => assert(convertToEqualizer(err) === testResult.err))) *>
            IO(assert(result == r))
        case Left(t) =>
          IO(logger.error("failed with:", t)) *>
            IO(fail(t))
      }
    })

  def succeedWithOutput(out: String, err: Option[String] = None) =
    succeedWith[Any]((), Some(out), err)

  def failWith[T](check: Throwable => Assertion, out: Option[String] = None, err: Option[String] = None) =
    TestFailureCheck[T](
      { testResult: TestResult[T] =>
        testResult.result match {
          case Right(r) =>
            IO(fail(s"expected failure, got $testResult"))
          case Left(t) =>
            IO(out.map(out => assert(convertToEqualizer(out) === testResult.out))) *>
              IO(err.map(err => assert(convertToEqualizer(err) === testResult.err))) *>
              IO(check(t))
        }
      })

  def failWithThrowable[T, X <: Throwable : ClassTag](f: X => Assertion = { _: X => Succeeded }, out: Option[String] = None, err: Option[String] = None) =
    failWith[T]({
      case x: X =>
        f(x)
      case x: Throwable =>
        fail(s"wrong exception is thrown: ", x)
    }, out, err)

  def failCommandExecutionWith[X <: Throwable : ClassTag](f: X => Assertion = { _: X => Succeeded }, out: Option[String] = None, err: Option[String] = None) =
    failWithThrowable[(Option[String], Any), X](f, out, err)

  def failWithUsageHelpRequested[T](help: String) =
    failWithThrowable[T, UsageHelpRequested](_ => Succeeded, out = Some(help), None)

  def failWithVersionHelpRequested[T](version: String) =
    failWithThrowable[T, VersionHelpRequested](_ => Succeeded, out = Some(version), None)

  def failCommandLineParsingWith[T](message: String) =
    failWithThrowable[T, CommandLineParsingFailed](t => assert(t.getMessage == message))

  def failSubcommandLineParsingWith[T](message: String) =
    failWithThrowable[T, CommandLineParsingFailedForSubcommand](t => assert(t.getMessage == message))

}
