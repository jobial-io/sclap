package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class ArithmeticExample2Test extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(ArithmeticExample2)(
      Seq() -> failSubcommandLineParsingWith("parsing failed for subcommand div"),
      Seq("add", "2", "3") -> succeedWithOutput("5\n")
    )
  }
}
