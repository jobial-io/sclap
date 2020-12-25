package io.jobial.sclap

import cats.effect.IO
import org.scalatest.flatspec.AsyncFlatSpec


class CommandLineNoImplicitsTest
  extends AsyncFlatSpec
    with CommandLineParserTestHelperNoImplicits {

  // Import specific implicits for parsing/printing the options

  import implicits.stringArgumentValueParser
  import implicits.intArgumentValueParser
  import cats.implicits.catsStdShowForInt
  import implicits.argumentValuePrinterFromShow


  "opt without default value test" should behave like {
    val spec = for {
      a <- opt[String]("--a").build
    } yield IO {
      a
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(None)),
      TestCase(Seq("--a", "hello"), succeedWith(Some("hello"))),
      TestCase(Seq("--a"), failCommandLineParsingWith("Missing required parameter for option '--a' (PARAM)"))
    )
  }
}
