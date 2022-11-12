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

  def runCommandLineTest[A](main: IO[A], args: String*)(assertion: TestResult[A] => IO[Assertion]): IO[Assertion] =
    for {
      result <- captureOutput {
        main.redeem(Left[Throwable, A](_), Right(_)).unsafeRunSync
      }
      r <- assertion(TestResult(result.result.flatMap(x => x), result.out, result.err))
    } yield r

  def runCommandLineTest[A](spec: CommandLineArgSpec[IO[A]], args: String*)(assertion: TestResult[A] => IO[Assertion]): IO[Assertion] =
    runCommandLineTest(executeCommandLine(spec, args.toList, useColors = false), args: _*)(assertion)

  def runCommandLineTest(app: CommandLineAppNoImplicits, args: String*)(assertion: TestResult[Any] => IO[Assertion]): IO[Assertion] =
    runCommandLineTest(app.run, args: _*)(assertion)

  def runCommandLineTest(app: {def main(args: Array[String]): Unit}, args: String*)(assertion: TestResult[Unit] => IO[Assertion]): IO[Assertion] =
    runCommandLineTest(
      for {
        app <- createNewInstanceOf(app)
      } yield app.main(args.toArray),
      args: _*
    )(assertion)

  implicit def assertionToIO(assertion: Assertion) = IO(assertion)

  def runCommandLineTestCases[A](spec: CommandLineArgSpec[IO[A]])(testCases: (Seq[String], TestCheck[A])*) =
    for {
      (args, check) <- testCases
    } yield
      it should s"${check.behave} for args: ${args.mkString(" ")}" in {
        runCommandLineTest(spec, args: _*)(check.assertion).unsafeToFuture
      }

  def runCommandLineTestCases(app: CommandLineAppNoImplicits)(testCases: (Seq[String], TestCheck[Any])*): Any =
    runCommandLineTestCases[Any](app.run)(testCases: _*)

  def succeedWith[T](assertion: T => IO[Assertion], out: Option[String], err: Option[String]) =
    TestSuccessCheck({ testResult: TestResult[T] =>
      testResult.result match {
        case Right(r) =>
          IO(logger.debug(r.toString)) *>
            IO(out.map(out => assert(convertToEqualizer(out) === testResult.out))) *>
            IO(err.map(err => assert(convertToEqualizer(err) === testResult.err))) *>
            assertion(r)
        case Left(t) =>
          IO(logger.error("failed with:", t)) *>
            IO(fail(t))
      }
    })

  def succeed(out: Option[String] = None, err: Option[String] = None) =
    succeedWith[Any]((), out, err)

  def succeedWithOutput(out: String, err: Option[String] = None) =
    succeedWith[Any]((), Some(out), err)

  def succeedWith(assertion: IO[Assertion]): TestSuccessCheck[Any] =
    succeedWith({ _: Any => assertion }, None, None)

  def succeedWith(assertion: IO[Assertion], out: String): TestSuccessCheck[Any] =
    succeedWith({ _: Any => assertion }, Some(out), None)

  def succeedWith(assertion: IO[Assertion], out: String, err: String): TestSuccessCheck[Any] =
    succeedWith({ _: Any => assertion }, Some(out), Some(err))

  def succeedWith[T](result: T, out: Option[String] = None, err: Option[String] = None): TestSuccessCheck[T] =
    succeedWith(r => IO(assert(result == r)), out, err)

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

  def failWith[T](check: Throwable => Assertion, out: String => Boolean, err: String => Boolean) =
    TestFailureCheck[T](
      { testResult: TestResult[T] =>
        testResult.result match {
          case Right(r) =>
            IO(fail(s"expected failure, got $testResult"))
          case Left(t) =>
            IO(assert(out(testResult.out))) *>
              IO(assert(err(testResult.err))) *>
              IO(check(t))
        }
      })

  def failWithThrowable[T, X <: Throwable : ClassTag](f: X => Assertion = { _: X => Succeeded }, out: Option[String] = None, err: Option[String] = None) =
    failWith[T]({
      case x: X =>
        f(x)
      case x: Throwable =>
        fail(s"wrong exception is thrown: ", x)
    }: Throwable => Assertion, out, err)

  def failWithThrowable[T, X <: Throwable : ClassTag](f: X => Assertion, out: String => Boolean, err: String => Boolean) =
    failWith[T]({
      case x: X =>
        f(x)
      case x: Throwable =>
        fail(s"wrong exception is thrown: ", x)
    }: Throwable => Assertion, out, err)

  def failCommandExecutionWith[X <: Throwable : ClassTag](f: X => Assertion = { _: X => Succeeded }, out: Option[String] = None, err: Option[String] = None) =
    failWithThrowable[(Option[String], Any), X](f, out, err)

  def failWithUsageHelpRequested[T](help: String) =
    failWithThrowable[T, UsageHelpRequested]({ _: Throwable => Succeeded }, out = Some(help), None)

  def failWithVersionHelpRequested[T](version: String) =
    failWithThrowable[T, VersionHelpRequested]({ _: Throwable => Succeeded }, out = Some(version), None)

  def failCommandLineParsingWith[T](message: String) =
    failWithThrowable[T, CommandLineParsingFailed] { t: Throwable => assert(t.getMessage == message) }

  def failSubcommandLineParsingWith[T](message: String) =
    failWithThrowable[T, CommandLineParsingFailedForSubcommand] { t: Throwable => assert(t.getMessage == message) }

  def createNewInstanceOf[T <: {def main(args: Array[String]): Unit}](o: T) =
    createNewInstanceOfWithConstructor(o) { classOfApp =>
      val c = classOfApp.getDeclaredConstructor()
      c.setAccessible(true)
      c.newInstance()
    } orElse
      createNewInstanceOfWithConstructor(o) { classOfApp =>
        val c = classOfApp.getDeclaredConstructor(getClass)
        c.setAccessible(true)
        c.newInstance(this)
      }

  def createNewInstanceOfWithConstructor[T <: {def main(args: Array[String]): Unit}](o: T)(const: Class[T] => T) = IO {
    val before = System.identityHashCode(o)
    val newO = const(o.getClass.asInstanceOf[Class[T]])
    val after = System.identityHashCode(newO)
    assert(after != before)
    newO match {
      case newO: App =>
        newO.delayedInit()
      case _ =>
    }
    newO.asInstanceOf[T]
  }
}
