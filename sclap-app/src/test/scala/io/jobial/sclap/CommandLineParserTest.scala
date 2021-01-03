/*
 * Copyright (c) 2020 Jobial OÜ. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package io.jobial.sclap

import cats.effect.IO
import org.scalatest.flatspec.AsyncFlatSpec

import scala.concurrent.duration._

class CommandLineParserTest
  extends AsyncFlatSpec
    with CommandLineParserTestHelper {

  "opt without default value test" should behave like {
    val spec =
      for {
        a <- opt[String]("a").alias("long")
      } yield IO {
        a
      }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(None)),
      TestCase(Seq("-a", "hello"), succeedWith(Some("hello"))),
      TestCase(Seq("--long", "hello"), succeedWith(Some("hello"))),
      TestCase(Seq("-a"), failCommandLineParsingWith("Missing required parameter for option '--long' (PARAM)"))
    )
  }

  "parsing opt with default value test" should behave like {
    val spec =
      for {
        a <- opt("--a", "hello")
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

  "parsing multiple opts test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a")
        b <- opt("--b", "hello")
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

  "parsing param without default value test" should behave like {
    val spec =
      for {
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

  "parsing param with default value test" should behave like {
    val spec =
      for {
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

  "parsing param with required value test" should behave like {
    val spec =
      for {
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

  "parsing params with explicit index test" should behave like {
    val spec =
      for {
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

  "parsing params without explicit index test" should behave like {
    val spec =
      for {
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

  "parsing subcommands test" should behave like {
    val subcommand1 =
      for {
        a <- opt[String]("--a")
      } yield IO {
        a
      }

    val subcommand2 = for {
      b <- opt("-b", 1)
    } yield IO {
      b

    }

    val spec =
      for {
        c <- opt[String]("--c")
        s1 <- subcommand("s1")(subcommand1)
        s2 <- subcommand("s2").aliases("s3")(subcommand2)
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
      TestCase(Seq("s2", "-b", "2"), succeedWith((None, 2))),
      TestCase(Seq("--c", "hi", "s2", "-b", "3"), succeedWith((Some("hi"), 3)))
    )
  }

  "parsing opt with standard extensions test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a").paramLabel("<a>")
        b <- opt("--b", 1).paramLabel("<b>")
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

  "parsing required opt test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a").required.paramLabel("<a>")
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

  "parsing opt with the picocli builder directly specified through the impl specific extension test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a").withPicocliOptionSpecBuilder(_.defaultValue("hello").paramLabel("<a>"))
        b <- opt("--b", 1).withPicocliOptionSpecBuilder(_.paramLabel("<b>"))
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

  "parsing command and subcommand properties test" should behave like {
    val sub =
      command.header("A subcommand").description("This is a subcommand.") {
        for {
          a <- opt[String]("--a")
        } yield IO {
          a
        }
      }

    val main =
      command.header("Main command with a subcommand").description("This is the main command.") {
        for {
          b <- opt[String]("--b")
          s <- subcommand("s")(sub)
        } yield for {
          r <- s
        } yield (b, r)
      }

    runCommandLineTestCases(main)(
      TestCase(Seq(), failSubcommandLineParsingWith("parsing failed for subcommand s")),
      TestCase(Seq("--c", "hello"), failCommandLineParsingWith("Unknown options: '--c', 'hello'")),
      TestCase(Seq("--help"), failWithUsageHelpRequested("""Main command with a subcommand
Usage: <main class> [-h] [--b=PARAM] [COMMAND]
This is the main command.
      --b=PARAM
  -h, --help      Show this help message and exit.
Commands:
  s  A subcommand
""")),
      TestCase(Seq("s", "--help"), succeedWith((None, None), Some("""A subcommand
Usage: <main class> s [-h] [--a=PARAM]
This is a subcommand.
      --a=PARAM
  -h, --help      Show this help message and exit.
"""), Some("")))
    )
  }

  "parsing a command with no command line options test" should behave like {
    val main =
      command.header("Main command with no args").description("This is the main command.") {
        IO {
          "hello"
        }
      }

    runCommandLineTestCases(main)(
      TestCase(Seq(), succeedWith("hello")),
      TestCase(Seq("--help"), failWithUsageHelpRequested("""Main command with no args
Usage: <main class> [-h]
This is the main command.
  -h, --help   Show this help message and exit.
"""))
    )
  }

  "accessing the full list of command line args along with options test" should behave like {
    val main =
      command.header("Main command with no args").description("This is the main command.") {
        for {
          a <- opt("--a", 1)
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

  "opts of built-in types" should behave like {
    val spec =
      for {
        s <- opt[String]("s", "")
        b <- opt[Boolean]("b", true)
        i <- opt[Int]("i", 1)
        l <- opt[Long]("l", 2)
        f <- opt[Float]("f", 1.0f)
        d <- opt[Double]("d", 1.0)
        g <- opt[Duration]("g", 1 second)
        h <- opt[FiniteDuration]("k", 2 seconds)
        j <- opt[BigDecimal]("j", BigDecimal(1.0))
      } yield IO {
        (s, b, i, l, f, d, g, h, j)
      }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(("", true, 1, 2, 1.0f, 1.0, 1 second, 2 seconds, BigDecimal(1.0)))),
      TestCase(Seq("-s", "", "-i", "1", "-l", "2", "-f", "1.0f", "-d", "1.0", "-g", "1 second", "-k", "2 seconds", "-j", "1.0"), succeedWith(("", true, 1, 2, 1.0f, 1.0, 1 second, 2 seconds, BigDecimal(1.0)))),
      TestCase(Seq("--help"), failWithUsageHelpRequested("""Usage: <main class> [-bh] [-d=PARAM] [-f=PARAM] [-g=PARAM] [-i=PARAM]
                    [-j=PARAM] [-k=PARAM] [-l=PARAM] [-s=PARAM]
  -b           (default: true).
  -d=PARAM     (default: 1.0).
  -f=PARAM     (default: 1.0).
  -g=PARAM     (default: 1 second).
  -h, --help   Show this help message and exit.
  -i=PARAM     (default: 1).
  -j=PARAM     (default: 1.0).
  -k=PARAM     (default: 2 seconds).
  -l=PARAM     (default: 2).
  -s=PARAM     (default: ).
"""))
    )
  }

  "params of built-in types" should behave like {
    val spec =
      for {
        s <- param[String]("")
        b <- param[Boolean](true)
        i <- param[Int](1)
        l <- param[Long](2)
        f <- param[Float](3.0f)
        d <- param[Double](4.0)
        g <- param[Duration](1 second)
        h <- param[FiniteDuration](2 seconds)
        j <- param[BigDecimal](BigDecimal(1.0))
      } yield IO {
        (s, b, i, l, f, d, g, h)
      }

    runCommandLineTestCases(spec)(
      TestCase(Seq("", "true", "1", "2", "1.0", "1.0", "1 second", "2 seconds"), succeedWith(("", true, 1, 2, 1.0f, 1.0, 1 second, 2 seconds)))
    )
  }

  "turning off default help option" should behave like {
    val main =
      command.header("Main command").description("This is the main command.")
        .help(false).commandLine {
        for {
          a <- opt("-a", 1)
          b <- param[String].paramLabel("xxxx")
          args <- args
        } yield IO {
          (a, b, args)
        }
      }

    runCommandLineTestCases(main)(
      TestCase(Seq(), succeedWith((1, None, List()))),
      TestCase(Seq("-a", "2", "hello"), succeedWith((2, Some("hello"), List("-a", "2", "hello")))),
      TestCase(Seq("--help"), failCommandLineParsingWith("Unknown option: '--help'"))
    )
  }

  "adding version help option" should behave like {
    val main =
      command.header("Main command").description("This is the main command.")
        .version("1.0").commandLine {
        for {
          a <- opt("-a", 1)
          b <- param[String].paramLabel("xxxx")
          args <- args
        } yield IO {
          (a, b, args)
        }
      }

    runCommandLineTestCases(main)(
      TestCase(Seq(), succeedWith((1, None, List()))),
      TestCase(Seq("-a", "2", "hello"), succeedWith((2, Some("hello"), List("-a", "2", "hello")))),
      TestCase(Seq("-h"), failWithUsageHelpRequested("""Main command
Usage: <main class> [-hV] [-a=PARAM] xxxx
This is the main command.
      xxxx
  -a=PARAM        (default: 1).
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
""")),
      TestCase(Seq("--version"), failWithVersionHelpRequested("""1.0
"""))
    )
  }

  "clustering posix styled short options" should behave like {
    val spec =
      for {
        create <- opt[Boolean]("-c")
        verbose <- opt[Boolean]("-v")
        file <- opt[String]("-f")
      } yield IO {
        (create, verbose, file)
      }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith((None, None, None))),
      TestCase(Seq("-c", "-v", "-f", "file.tar.gz"), succeedWith((Some(true), Some(true), Some("file.tar.gz")))),
      TestCase(Seq("-cvf", "file.tar.gz"), succeedWith((Some(true), Some(true), Some("file.tar.gz"))))
    )
  }

  "clustering posix styled short options turned off" should behave like {
    val spec =
      command.clusteredShortOptionsAllowed(false) {
        for {
          create <- opt[Boolean]("-c")
          verbose <- opt[Boolean]("-v")
          file <- opt[String]("-f")
        } yield IO {
          (create, verbose, file)
        }
      }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith((None, None, None))),
      TestCase(Seq("-c", "-v", "-f", "file.tar.gz"), succeedWith((Some(true), Some(true), Some("file.tar.gz")))),
      TestCase(Seq("-cvf", "file.tar.gz"), failCommandLineParsingWith("Unknown options: '-cvf', 'file.tar.gz'"))
    )
  }

  "automatic prefixes turned off" should behave like {
    val spec =
      command.prefixLongOptionsWith(None).prefixShortOptionsWith(None) {
        for {
          a <- opt[String]("a").alias("long")
        } yield IO {
          a
        }
      }

    runCommandLineTestCases(spec)(
      TestCase(Seq(), succeedWith(None)),
      TestCase(Seq("-a", "hello"), failCommandLineParsingWith("Unmatched arguments from index 0: '-a', 'hello'")),
      TestCase(Seq("a", "hello"), succeedWith(Some("hello"))),
      TestCase(Seq("--long", "hello"), failCommandLineParsingWith("Unmatched arguments from index 0: '--long', 'hello'")),
      TestCase(Seq("long", "hello"), succeedWith(Some("hello"))),
      TestCase(Seq("a"), failCommandLineParsingWith("Missing required parameter for option 'long' (PARAM)")),
      TestCase(Seq("-a"), failCommandLineParsingWith("Unmatched argument at index 0: '-a'"))
    )
  }


}
