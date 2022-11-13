package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class TarExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(TarExample)(
      Seq() -> succeedWithOutput("tar create None verbose None file None\n"),
      Seq("-f", "test") -> succeedWithOutput("tar create None verbose None file Some(test)\n"),
      Seq("--help") -> failWithUsageHelpRequested("""Tar Archive
Usage: TarExampleTest [-c] [-h] [-v] [-f=PARAM]
This is a utility to manage tar archives.
  -c
  -f, --file=PARAM
  -h, --help         Show this help message and exit.
  -v
""")
    )
  }
}
