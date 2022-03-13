package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class ZIOExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(ZIOExample)(
      Seq() -> succeedWithOutput("Hello None\n"),
      Seq("--hello", "world") -> succeedWithOutput("Hello Some(world)\n")
    )
  }
}
