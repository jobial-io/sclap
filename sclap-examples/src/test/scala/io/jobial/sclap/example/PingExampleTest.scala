package io.jobial.sclap.example

import io.jobial.sclap.CommandLineParserTestHelper
import org.scalatest.flatspec.AsyncFlatSpec

class PingExampleTest extends AsyncFlatSpec
  with CommandLineParserTestHelper {

  "app" should behave like {
    runCommandLineTestCases(PingExample)(
      Seq() -> failCommandLineParsingWith("Missing required parameter: '<hostname>'"),
      Seq("localhost") -> succeedWithOutput("Pinging localhost with count: None, timeout: 5 seconds...\n"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: PingExampleTest [-h] [--count=PARAM] [--timeout=PARAM] <hostname>
      <hostname>        The host.
      --count=PARAM     Number of packets.
  -h, --help            Show this help message and exit.
      --timeout=PARAM   The timeout (default: 5 seconds).
""")
    )
  }
}
