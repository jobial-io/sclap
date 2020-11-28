package io.jobial.sclap

import cats.effect.IO
import com.sun.prism.Texture.Usage
import io.jobial.sclap.core.{CommandLineArgSpec, CommandLineParsingFailed, CommandLineParsingFailedForSubcommand, UsageHelpRequested}
import org.scalatest.{Assertion, Succeeded}
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import picocli.CommandLine

import java.io.{ByteArrayOutputStream, PrintStream}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}


case class TestCase[T](args: Seq[String], check: TestResult[T] => Assertion)

case class TestResult[T](result: Try[T], out: String, err: String)

trait CommandLineParserTestUtils extends CommandLineParser {
  this: AsyncFlatSpec =>

  def runCommandLineTestCases[A](spec: CommandLineArgSpec[IO[A]])(testCases: TestCase[A]*) =
    for {
      t <- testCases
    } yield
      it should s"work for args: ${t.args.mkString(" ")}" in {
        val out = new ByteArrayOutputStream()
        val err = new ByteArrayOutputStream()
        t.check(TestResult(Try(executeCommandLine(spec, t.args, new PrintStream(out), new PrintStream(err)).unsafeRunSync), out.toString, err.toString))
      }

  def succeedWith[T](result: T, out: Option[String] = None, err: Option[String] = None)(testResult: TestResult[T]) = testResult.result match {
    case Success(r) =>
      logger.debug(r.toString)
      out.map(out => assert(out === testResult.out))
      err.map(err => assert(err === testResult.err))
      assert(result == r)
    case Failure(t) =>
      t.printStackTrace
      logger.error("failed with:", t)
      fail(t)
  }

  def failWith[T](check: Throwable => Assertion)(testResult: TestResult[T]) = {
    logger.debug("expects failure: ", testResult.toString)
    testResult.result match {
      case Success(r) =>
        fail(s"expected failure, got $testResult")
      case Failure(t) =>
        check(t)
    }
  }

  def failCommandLineParsingWithThrowable[T <: Throwable : ClassTag](f: String => Assertion = { _ => Succeeded }) =
    failWith {
      case x: T =>
        println(x.getMessage)
        f(x.getMessage)
      case x: Throwable =>
        fail(s"wrong exception is thrown: ", x)
    }(_: TestResult[_])

  def failWithUsageHelpRequested(message: String) =
    failCommandLineParsingWithThrowable[UsageHelpRequested](m => assert(m == message))

  def failCommandLineParsingWith(message: String) =
    failCommandLineParsingWithThrowable[CommandLineParsingFailed](m => assert(m == message))

  def failSubcommandLineParsingWith(message: String) =
    failCommandLineParsingWithThrowable[CommandLineParsingFailedForSubcommand](m => assert(m == message))

}
