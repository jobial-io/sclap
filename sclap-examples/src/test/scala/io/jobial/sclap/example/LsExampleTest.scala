package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class LsExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(LsExample)(
      Seq() -> succeedWithOutput("called ls with false and None...\n"),
      Seq("-l", "test") -> succeedWithOutput("called ls with true and Some(test)...\n"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: LsExampleTest [-hl] <dir>
      <dir>    The directory.
  -h, --help   Show this help message and exit.
  -l, --long   Long format (default: false).
""")
    )
  }
}
