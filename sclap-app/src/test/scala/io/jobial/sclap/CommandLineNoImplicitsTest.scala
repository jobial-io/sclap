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
import org.scalatest.flatspec.AsyncFlatSpec


class CommandLineNoImplicitsTest
  extends AsyncFlatSpec
    with CommandLineParserTestHelperNoImplicits {

  // Import specific implicits for parsing/printing the options

  import implicits.stringArgumentValueParser
  import implicits.intArgumentValueParser
  import cats.implicits.catsStdShowForInt
  import implicits.argumentValuePrinterFromShow


  "opt without default value test" should behave like {
    val spec = for {
      a <- opt[String]("--a").build
    } yield IO {
      a
    }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith(None),
      Seq("--a", "hello") -> succeedWith(Some("hello")),
      Seq("--a") -> failCommandLineParsingWith("Missing required parameter for option '--a' (PARAM)")
    )
  }
}
