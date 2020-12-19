package io.jobial.sclap

import cats.effect.IO
import io.jobial.sclap.core.CommandLine
import org.scalatest.flatspec.AsyncFlatSpec

import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.{ExecutionContext, Future}
import concurrent.duration._

trait CommandLineParserFutureTestBase
  extends AsyncFlatSpec
    with CommandLineParserTestHelper {

  def sideEffectExpected: Boolean

  /**
   * Returns a Future that increments the given counter. The function is used in the subsequent test cases to assess if
   * any Future gets executed as a side-effect of parsing the command line spec with Future as the return type.
   *
   * The unbounded thread pool is needed to avoid false positives caused by implicit ordering potentially introduced by 
   * a bounded thread pool.
   *
   * @param counter
   * @return
   */
  def futureForCounter(counter: AtomicInteger) =
    Future(counter.incrementAndGet)(fromExecutor(newCachedThreadPool))

  implicit val timer = IO.timer(ExecutionContext.global)

  def checkSideEffects(counter: AtomicInteger, sideEffectCountInSpec: Int) = TestSuccessCheck { _: TestResult[Int] =>
    IO.sleep(1 second) *>
      IO(assert(if (sideEffectExpected) counter.get == sideEffectCountInSpec else counter.get == 0))
  }

  "command line that returns a future" should behave like {
    // Counter to signal side effect
    val counter = new AtomicInteger

    val spec: CommandLine[Int] =
      for {
        a <- opt[String]("a")
      } yield futureForCounter(counter)

    runCommandLineTestCases(spec)(
      TestCase(Seq("--hello"), checkSideEffects(counter, 1))
    )
  }

  "command line with two options that returns a future" should behave like {
    val counter = new AtomicInteger

    val spec: CommandLine[Int] =
      for {
        a <- opt[String]("a")
        b <- opt("b", 1)
      } yield futureForCounter(counter)

    runCommandLineTestCases(spec)(
      TestCase(Seq("--hello"), checkSideEffects(counter, 1))
    )
  }

  "command line with subcommand that returns a future" should behave like {
    val counter = new AtomicInteger

    val sub = subcommand[Int](
      "sub",
      {
        for {
          c <- opt[String]("c")
        } yield futureForCounter(counter)
      }
    )

    val spec: CommandLine[Int] =
      for {
        a <- opt[String]("a")
        b <- opt("b", 1)
        s <- sub
      } yield futureForCounter(counter)

    runCommandLineTestCases(spec)(
      TestCase(Seq("--hello"), checkSideEffects(counter, 2))
    )
  }
}
