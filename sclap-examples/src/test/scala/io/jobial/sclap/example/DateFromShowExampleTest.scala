package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class DateFromShowExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(DateFromShowExample)(
      Seq() -> succeed(),
      Seq("--date", "2022-03-13") -> succeedWithOutput("date: 2022-03-13\n")
    )
  }
}
