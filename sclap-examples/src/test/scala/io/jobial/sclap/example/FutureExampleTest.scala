package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class FutureExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(FutureExample)(
      Seq() -> succeedWithOutput("hello None\n"),
      Seq("--hello", "world") -> succeedWithOutput("hello Some(world)\n")
    )
  }
}
