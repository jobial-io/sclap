package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class DateExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "opt without default value test" should behave like {
    runCommandLineTestCases(DateExample)(
      Seq() -> succeed(),
      Seq("--date", "2022-03-13") -> succeedWithOutput("date: 2022-03-13\n")
    )
  }
}
