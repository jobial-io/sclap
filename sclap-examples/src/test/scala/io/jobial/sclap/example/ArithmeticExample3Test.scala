package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class ArithmeticExample3Test extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "opt without default value test" should behave like {
    runCommandLineTestCases(ArithmeticExample3)(
      Seq() -> failSubcommandLineParsingWith("parsing failed for subcommand div"),
      Seq("add", "2", "3") -> succeedWithOutput("5\n")
    )
  }
}
