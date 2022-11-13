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
  override implicit def fromFuture[A](f: => Future[A]) = {
    // We force evaluate f here for the purposes of this test, causing the test version of fromFuture to be unsafe
    val forceEvaluated = f
    super.fromFuture(forceEvaluated)
  }
}
