package io.jobial.sclap

import cats.effect.IO
import cats.implicits._
import io.jobial.sclap.core.UsageHelpRequested
import io.jobial.sclap.implicits._
import org.scalatest.flatspec.AsyncFlatSpec

class CommandLineParserTest
  extends AsyncFlatSpec
    with CommandLineParserTestUtils {

  "parsing opt without default value" should behave like {
    val spec = for {
      a <- opt[String]("a")
    } yield IO {
      a
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(None)),
      TestCase(Seq("--a", "hello"), succeedWith(Some("hello"))),
      TestCase(Seq("--a"), failCommandLineParsingWith("Missing required parameter for option '--a' (PARAM)"))
    )
  }

  "parsing opt with default value" should behave like {
    val spec = for {
      a <- opt("a", "hello")
    } yield IO {
      a
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith("hello")),
      TestCase(Seq("--a"), failCommandLineParsingWith("Missing required parameter for option '--a' (PARAM)")),
      TestCase(Seq("--a", "hi"), succeedWith("hi")),
      TestCase(Seq("--b"), failCommandLineParsingWith("Unknown option: '--b'"))
    )
  }

  "parsing multiple opts" should behave like {
    val spec = for {
      a <- opt[String]("a")
      b <- opt("b", "hello")
    } yield IO {
      (a, b)
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith((None, "hello"))),
      TestCase(Seq("--a"), failCommandLineParsingWith("Missing required parameter for option '--a' (PARAM)")),
      TestCase(Seq("--a", "hi"), succeedWith((Some("hi"), "hello"))),
      TestCase(Seq("--b"), failCommandLineParsingWith("Missing required parameter for option '--b' (PARAM)"))
    )
  }

  "parsing param without default value" should behave like {
    val spec = for {
      a <- param[String]
    } yield IO {
      a
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(None)),
      TestCase(Seq("hello"), succeedWith(Some("hello"))),
      TestCase(Seq("hi", "hello"), failCommandLineParsingWith("Unmatched argument at index 1: 'hello'"))
    )
  }

  "parsing param with default value" should behave like {
    val spec = for {
      a <- param[String].defaultValue("x")
      b <- param[String].defaultValue("y")
    } yield IO {
      a + b
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith("xy")),
      TestCase(Seq("1", "2"), succeedWith("12"))
    )
  }

  "parsing param with required value" should behave like {
    val spec = for {
      a <- param[String].required
      b <- param[String].required
    } yield IO {
      a + b
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), failCommandLineParsingWith("Missing required parameters: 'PARAM', 'PARAM'")),
      TestCase(Seq("1", "2"), succeedWith("12"))
    )
  }

  "parsing params with explicit index" should behave like {
    val spec = for {
      a <- param[Int].index(0)
      b <- param[Int].index(1)
    } yield IO {
      a |+| b
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(None)),
      TestCase(Seq("1", "2"), succeedWith(Some(3)))
    )
  }

  "parsing params without explicit index" should behave like {
    val spec = for {
      a <- param[Int]
      b <- param[Int]
    } yield IO {
      a |+| b
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(None)),
      TestCase(Seq("1", "2"), succeedWith(Some(3)))
    )
  }

  "parsing subcommands" should behave like {
    val subcommand1 = for {
      a <- opt[String]("a")
    } yield IO {
      a
    }

    val subcommand2 = for {
      b <- opt("b", 1)
    } yield IO {
      b

    }

    val spec = for {
      c <- opt[String]("c")
      s1 <- subcommand("s1", subcommand1)
      s2 <- subcommand("s2", subcommand2).aliases("s3")
    } yield for {
      r <- s1 orElse s2
    } yield (c, r)

    runCommandLineTestCases(spec)(
      TestCase(Seq(), failSubcommandLineParsingWith("parsing failed for subcommand s2")),
      TestCase(Seq("--c", "hello"), failSubcommandLineParsingWith("parsing failed for subcommand s2")),
      TestCase(Seq("s1"), succeedWith((None, None))),
      TestCase(Seq("s1", "--a", "hi"), succeedWith((None, Some("hi")))),
      TestCase(Seq("--c", "hi", "s1", "--a", "hi"), succeedWith((Some("hi"), Some("hi")))),
      TestCase(Seq("s2"), succeedWith((None, 1))),
      TestCase(Seq("s2", "--b", "2"), succeedWith((None, 2))),
      TestCase(Seq("--c", "hi", "s2", "--b", "3"), succeedWith((Some("hi"), 3)))
    )
  }

  "parsing opt with standard extensions" should behave like {
    val spec = for {
      a <- opt[String]("a").paramLabel("<a>")
      b <- opt("b", 1).paramLabel("<b>")
    } yield IO {
      (a, b)
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith((None, 1))),
      TestCase(Seq("--a"), failCommandLineParsingWith("Missing required parameter for option '--a' (<a>)")),
      TestCase(Seq("--a", "hi"), succeedWith((Some("hi"), 1))),
      TestCase(Seq("--a", "hi", "--b", "2"), succeedWith((Some("hi"), 2))),
      TestCase(Seq("--c"), failCommandLineParsingWith("Unknown option: '--c'"))
    )
  }

  "parsing required opt" should behave like {
    val spec = for {
      a <- opt[String]("a").required.paramLabel("<a>")
    } yield IO {
      a
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), failCommandLineParsingWith("Missing required option: '--a=<a>'")),
      TestCase(Seq("--a"), failCommandLineParsingWith("Missing required parameter for option '--a' (<a>)")),
      TestCase(Seq("--a", "hi"), succeedWith("hi")),
      TestCase(Seq("--c"), failCommandLineParsingWith("Missing required option: '--a=<a>'")),
      TestCase(Seq("--a", "x", "--c"), failCommandLineParsingWith("Unknown option: '--c'"))
    )
  }

  "parsing opt with the picocli builder directly specified through the impl specific extension" should behave like {
    val spec = for {
      a <- opt[String]("a").withPicocliOptionSpecBuilder(_.defaultValue("hello").paramLabel("<a>"))
      b <- opt("b", 1).withPicocliOptionSpecBuilder(_.paramLabel("<b>"))
      //      p <- param[String]("p").withPicocliOptionSpecBuilder(_.defaultValue("hello"))
    } yield IO {
      (a, b)
    }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith((Some("hello"), 1))),
      TestCase(Seq("--a"), failCommandLineParsingWith("Missing required parameter for option '--a' (<a>)")),
      TestCase(Seq("--a", "hi"), succeedWith((Some("hi"), 1))),
      TestCase(Seq("--a", "hi", "--b", "2"), succeedWith((Some("hi"), 2))),
      TestCase(Seq("--c"), failCommandLineParsingWith("Unknown option: '--c'"))
    )
  }

  "parsing command and subcommand properties" should behave like {
    val sub =
      command.header("A subcommand").description("This is a subcommand.") {
        for {
          a <- opt[String]("a")
        } yield IO {
          a
        }
      }

    val main =
      command.header("Main command with a subcommand").description("This is the main command.") {
        for {
          b <- opt[String]("b")
          s <- subcommand("s", sub)
        } yield for {
          r <- s
        } yield (b, r)
      }

    runCommandLineTestCases(main)(
      TestCase(Seq(), failSubcommandLineParsingWith("parsing failed for subcommand s")),
      TestCase(Seq("--c", "hello"), failCommandLineParsingWith("Unknown options: '--c', 'hello'")),
      TestCase(Seq("--help"), failWithUsageHelpRequested("Usage help requested")),
      TestCase(Seq("s", "--help"), succeedWith((None, None), Some("""A subcommand
Usage: <main class> s [-hV] [--a=PARAM]
This is a subcommand.
      --a=PARAM
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
"""), Some("")))
    )
  }

  "parsing a command with no command line options" should behave like {
    val main =
      command.header("Main command with no args").description("This is the main command.") {
        IO {
          "hello"
        }
      }

    runCommandLineTestCases(main)(
      TestCase(Seq(), succeedWith("hello")),
      TestCase(Seq("--help"), failCommandLineParsingWithThrowable[UsageHelpRequested]())
    )
  }

  "accessing the full list of command line args along with options" should behave like {
    val main =
      command.header("Main command with no args").description("This is the main command.") {
        for {
          a <- opt("a", 1)
          b <- param[String].paramLabel("xxxx")
          args <- args
        } yield IO {
          (a, b, args)
        }
      }

    runCommandLineTestCases(main)(
      TestCase(Seq(), succeedWith((1, None, List()))),
      TestCase(Seq("--a", "2", "hello"), succeedWith((2, Some("hello"), List("--a", "2", "hello"))))
    )
  }

}
