package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class ArithmeticExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(ArithmeticExample)(
      Seq() -> failSubcommandLineParsingWith("parsing failed for subcommand div"),
      Seq("add", "-a", "2", "-b", "3") -> succeedWithOutput("5\n")
    )
  }
}
