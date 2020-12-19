package io.jobial.sclap

import scala.concurrent.{ExecutionContext, Future}

class CommandLineParserFutureUnsafeImplicitTest
  extends CommandLineParserFutureTestBase {

  val sideEffectExpected = true

  /**
   * Warning: This override intentionally evaluates the Future argument eagerly, which is not safe in general. The purpose of this 
   * 'negative' test suite is to make sure the regular tests are not reporting false positives when tested with the safe 
   * version of fromFuture.
   *
   * @param f
   * @param ec
   * @tparam A
   * @return
   */
  override implicit def fromFuture[A](f: => Future[A])(implicit ec: ExecutionContext) = {
    // We force evaluate f here for the purposes of this test, causing the test version of fromFuture to be unsafe
    val forceEvaluated = f
    super.fromFuture(forceEvaluated)(ec)
  }
}
