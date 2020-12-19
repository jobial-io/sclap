package io.jobial.sclap

import cats.effect.IO
import cats.implicits._
import io.jobial.sclap.core.{CommandLineArgSpec, CommandLineParsingFailed, CommandLineParsingFailedForSubcommand, UsageHelpRequested}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.{Assertion, Succeeded}

import java.io.{ByteArrayOutputStream, PrintStream}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}


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

case class TestResult[T](result: Try[T], out: String, err: String)

trait CommandLineParserTestHelperNoImplicits extends CommandLineParserNoImplicits {
  this: AsyncFlatSpec =>

  def runCommandLineTestCases[A](spec: CommandLineArgSpec[IO[A]])(testCases: TestCase[A]*) =
    for {
      t <- testCases
    } yield
      it should s"${t.check.behave} for args: ${t.args.mkString(" ")}" in {
        val out = new ByteArrayOutputStream
        val err = new ByteArrayOutputStream
        val checkResult =
          for {
            result <- executeCommandLine(spec, t.args, new PrintStream(out), new PrintStream(err))
              .redeem(Failure[A](_), Success(_))
            r <- t.check.assertion(TestResult(result, out.toString, err.toString))
          } yield r
        checkResult.unsafeToFuture
      }

  def succeedWith[T](result: T, out: Option[String] = None, err: Option[String] = None) =
    TestSuccessCheck({ testResult: TestResult[T] =>
      testResult.result match {
        case Success(r) =>
          IO(logger.debug(r.toString)) *>
            IO(out.map(out => assert(out eqv testResult.out))) *>
            IO(err.map(err => assert(err eqv testResult.err))) *>
            IO(assert(result == r))
        case Failure(t) =>
          IO(logger.error("failed with:", t)) *>
            IO(fail(t))
      }
    })

  def failWith[T](check: Throwable => Assertion, out: Option[String] = None, err: Option[String] = None) =
    TestFailureCheck[T](
      { testResult: TestResult[T] =>
        testResult.result match {
          case Success(r) =>
            IO(fail(s"expected failure, got $testResult"))
          case Failure(t) =>
            IO(out.map(out => assert(out eqv testResult.out))) *>
              IO(err.map(err => assert(err eqv testResult.err))) *>
              IO(check(t))
        }
      })

  def failCommandLineParsingWithThrowable[T, X <: Throwable : ClassTag](f: String => Assertion = { _ => Succeeded }, out: Option[String] = None, err: Option[String] = None) =
    failWith[T]({
      case x: X =>
        f(x.getMessage)
      case x: Throwable =>
        fail(s"wrong exception is thrown: ", x)
    }, out, err)

  def failWithUsageHelpRequested[T](help: String) =
    failCommandLineParsingWithThrowable[T, UsageHelpRequested](_ => Succeeded, out = Some(help), None)

  def failCommandLineParsingWith[T](message: String) =
    failCommandLineParsingWithThrowable[T, CommandLineParsingFailed](m => assert(m == message))

  def failSubcommandLineParsingWith[T](message: String) =
    failCommandLineParsingWithThrowable[T, CommandLineParsingFailedForSubcommand](m => assert(m == message))

}
