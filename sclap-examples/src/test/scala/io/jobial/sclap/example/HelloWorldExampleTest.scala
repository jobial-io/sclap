package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class HelloWorldExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(HelloExample)(
      Seq() -> succeedWithOutput("hello None\n"),
      Seq("--hello", "world") -> succeedWithOutput("hello Some(world)\n"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: HelloWorldExampleTest [-h] [--hello=PARAM]
  -h, --help          Show this help message and exit.
      --hello=PARAM
""")
    )
  }
}
