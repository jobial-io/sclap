package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class HelloWorldExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(HelloWorldExample)(
      Seq() -> succeedWithOutput("hello world\n"),
      Seq("--hello", "there") -> succeedWithOutput("hello there\n"),
      Seq("--help") -> failWithUsageHelpRequested("""Hello World
Usage: HelloWorldExampleTest [-h] [--hello=PARAM]
A hello world app with one option.
  -h, --help          Show this help message and exit.
      --hello=PARAM   (default: world).
""")
    )
  }
}
