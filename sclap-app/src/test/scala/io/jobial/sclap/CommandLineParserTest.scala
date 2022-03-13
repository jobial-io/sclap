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
import io.jobial.sclap.core.{ArgumentValueParser, ArgumentValuePrinter, IncorrectCommandLineUsage, IncorrectCommandLineUsageInSubcommand}
import org.scalatest.flatspec.AsyncFlatSpec

import java.time.LocalDate
import scala.concurrent.duration._
import scala.util.Try

class CommandLineParserTest
  extends AsyncFlatSpec
    with CommandLineParserTestHelper {

  "opt without default value test" should behave like {
    val spec =
      for {
        a <- opt[String]("a", "long")
      } yield IO {
        a
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith(None),
      Seq("-a", "hello") -> succeedWith(Some("hello")),
      Seq("--long", "hello") -> succeedWith(Some("hello")),
      Seq("-a") -> failCommandLineParsingWith("Missing required parameter for option '--long' (PARAM)")
    )
  }

  "parsing opt with default value test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a").default("hello")
        c <- opt[String]("c", "C").default("world")
      } yield IO {
        a
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith("hello"),
      Seq("--a") -> failCommandLineParsingWith("Missing required parameter for option '--a' (PARAM)"),
      Seq("--a", "hi") -> succeedWith("hi"),
      Seq("--b") -> failCommandLineParsingWith("Unknown option: '--b'"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-h] [--a=PARAM] [-c=PARAM]
      --a=PARAM   (default: hello).
  -c, -C=PARAM    (default: world).
  -h, --help      Show this help message and exit.
""")
    )
  }

  "parsing opt with description test" should behave like {
    val spec =
      for {
        a <- opt[String]("a").default("hello").description("This is option a.")
        b <- opt[String]("b").default("hello").description("This is option b")
        c <- opt[String]("c").description("This is option c.")
        d <- opt[String]("d").description("This is option d")
      } yield IO {
        (a, b, c, d)
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith(("hello", "hello", None, None)),
      Seq("-a", "hi") -> succeedWith(("hi", "hello", None, None)),
      Seq("-b", "hi") -> succeedWith(("hello", "hi", None, None)),
      Seq("-a", "hi", "-b", "hi") -> succeedWith(("hi", "hi", None, None)),
      Seq("-c", "hi") -> succeedWith(("hello", "hello", Some("hi"), None)),
      Seq("-d", "hi") -> succeedWith(("hello", "hello", None, Some("hi"))),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-h] [-a=PARAM] [-b=PARAM] [-c=PARAM] [-d=PARAM]
  -a=PARAM     This is option a (default: hello).
  -b=PARAM     This is option b (default: hello).
  -c=PARAM     This is option c.
  -d=PARAM     This is option d.
  -h, --help   Show this help message and exit.
""")
    )
  }

  "parsing multiple opts test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a")
        b <- opt[String]("--b").default("hello")
      } yield IO {
        (a, b)
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith((None, "hello")),
      Seq("--a") -> failCommandLineParsingWith("Missing required parameter for option '--a' (PARAM)"),
      Seq("--a", "hi") -> succeedWith((Some("hi"), "hello")),
      Seq("--b") -> failCommandLineParsingWith("Missing required parameter for option '--b' (PARAM)")
    )
  }

  "parsing param without default value test" should behave like {
    val spec =
      for {
        a <- param[String]
        b <- param[String].label("PARAM1")
        c <- param[String].label("PARAM2").description("This is PARAM2.")
      } yield IO {
        (a, b, c)
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith((none[String], none[String], none[String])),
      Seq("hello") -> succeedWith((Some("hello"), None, None)),
      Seq("hello", "hi") -> succeedWith((Some("hello"), Some("hi"), None)),
      Seq("hello", "hi", "ola") -> succeedWith((Some("hello"), Some("hi"), Some("ola"))),
      Seq("hello", "hi", "ola", "oi") -> failCommandLineParsingWith("Unmatched argument at index 3: 'oi'"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-h] PARAM PARAM1 PARAM2
      PARAM
      PARAM1
      PARAM2   This is PARAM2.
  -h, --help   Show this help message and exit.
""")
    )
  }

  "parsing param with default value test" should behave like {
    val spec =
      for {
        a <- param[String].default("x")
        b <- param[String].default("y").label("PARAM1")
        c <- param[String].default("z").label("PARAM2").description("This is PARAM2.")
      } yield IO {
        a + b + c
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith("xyz"),
      Seq("1", "2") -> succeedWith("12z"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-h] PARAM PARAM1 PARAM2
      PARAM    (default: x).
      PARAM1   (default: y).
      PARAM2   This is PARAM2 (default: z).
  -h, --help   Show this help message and exit.
""")
    )
  }

  "parsing param with required value test" should behave like {
    val spec =
      for {
        a <- param[String].required
        b <- param[String].required.label("PARAM1")
        c <- param[String].required.label("PARAM2").description("This is PARAM2")
      } yield IO {
        a + b + c
      }

    runCommandLineTestCases(spec)(
      Seq() -> failCommandLineParsingWith("Missing required parameters: 'PARAM', 'PARAM1', 'PARAM2'"),
      Seq("1", "2") -> failCommandLineParsingWith("Missing required parameter: 'PARAM2'"),
      Seq("1", "2", "3") -> succeedWith("123"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-h] PARAM PARAM1 PARAM2
      PARAM
      PARAM1
      PARAM2   This is PARAM2.
  -h, --help   Show this help message and exit.
""")
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
      Seq() -> succeedWith(None),
      Seq("1", "2") -> succeedWith(Some(3))
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
      Seq() -> succeedWith(None),
      Seq("1", "2") -> succeedWith(Some(3))
    )
  }

  "parsing subcommands test" should behave like {
    val subcommand1 =
      for {
        a <- opt[String]("--a")
      } yield
        if (a eqv Some("0"))
          IO.raiseError(new IllegalArgumentException("s1 failed"))
        else
          IO(a)

    val subcommand2 = for {
      b <- opt[Int]("-b").default(1)
    } yield
      if (b eqv 0)
        IO.raiseError(new IllegalArgumentException("s2 failed"))
      else if (b eqv 99)
        IO.raiseError(new IncorrectCommandLineUsage("b cannot be 99"))
      else
        IO(b)

    val spec =
      for {
        c <- opt[String]("--c")
        s1 <- subcommand("s1")(subcommand1)
        s2 <- subcommand("s2", "s3")(subcommand2)
      } yield
        if (c eqv Some("illegal"))
          IO.raiseError(IncorrectCommandLineUsage("c cannot be illegal"))
        else
          for {
            r <- s1 orElse s2
          } yield
            (c, r)

    runCommandLineTestCases(spec)(
      Seq() -> failSubcommandLineParsingWith("parsing failed for subcommand s2"),
      Seq("--c", "hello") -> failSubcommandLineParsingWith("parsing failed for subcommand s2"),
      Seq("s1") -> succeedWith((None, None)),
      Seq("s1", "--a", "hi") -> succeedWith((None, Some("hi"))),
      Seq("--c", "hi", "s1", "--a", "hi") -> succeedWith((Some("hi"), Some("hi"))),
      Seq("s2") -> succeedWith((None, 1)),
      Seq("s2", "-b", "2") -> succeedWith((None, 2)),
      Seq("--c", "hi", "s2", "-b", "3") -> succeedWith((Some("hi"), 3)),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-h] [--c=PARAM] [COMMAND]
      --c=PARAM
  -h, --help      Show this help message and exit.
Commands:
  s1
  s2, s3
"""),
      Seq("s1", "--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest s1 [-h] [--a=PARAM]
      --a=PARAM
  -h, --help      Show this help message and exit.
"""),
      Seq("s2", "--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest s2 [-h] [-b=PARAM]
  -b=PARAM     (default: 1).
  -h, --help   Show this help message and exit.
"""),
      Seq("s1", "--a", "0") -> failCommandExecutionWith[IllegalArgumentException](t => assert(t.getMessage == "s1 failed"),
        out = Some(""), err = Some("s1 failed")),
      Seq("s2", "-b", "0") -> failCommandExecutionWith[IllegalArgumentException](t => assert(t.getMessage == "s2 failed"),
        out = Some(""), err = Some("s2 failed")),
      Seq("s2", "-b", "99") -> failCommandExecutionWith[IncorrectCommandLineUsageInSubcommand](t => assert(t.getMessage == "b cannot be 99"),
        out = Some(""), err = Some("""b cannot be 99
Usage: CommandLineParserTest s2 [-h] [-b=PARAM]
  -b=PARAM     (default: 1).
  -h, --help   Show this help message and exit.
""")),
      Seq("--c", "illegal", "s2", "-b", "1") -> failCommandExecutionWith[IncorrectCommandLineUsage](t => assert(t.getMessage == "c cannot be illegal"),
        out = Some(""), err = Some("""c cannot be illegal
Usage: CommandLineParserTest [-h] [--c=PARAM] [COMMAND]
      --c=PARAM
  -h, --help      Show this help message and exit.
Commands:
  s1
  s2, s3
""")))

  }

  "parsing opt with standard extensions test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a").label("<a>")
        b <- opt[Int]("--b").default(1).label("<b>")
      } yield IO {
        (a, b)
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith((None, 1)),
      Seq("--a") -> failCommandLineParsingWith("Missing required parameter for option '--a' (<a>)"),
      Seq("--a", "hi") -> succeedWith((Some("hi"), 1)),
      Seq("--a", "hi", "--b", "2") -> succeedWith((Some("hi"), 2)),
      Seq("--c") -> failCommandLineParsingWith("Unknown option: '--c'")
    )
  }

  "parsing required opt test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a").required.label("<a>")
        b <- opt[String]("b", "B").required.label("<b>")
      } yield IO {
        a
      }

    runCommandLineTestCases(spec)(
      Seq() -> failCommandLineParsingWith("Missing required options: '--a=<a>', '-b=<b>'"),
      Seq("-b", "x", "--a") -> failCommandLineParsingWith("Missing required parameter for option '--a' (<a>)"),
      Seq("-b", "x", "--a", "hi") -> succeedWith("hi"),
      Seq("-b", "x", "--c") -> failCommandLineParsingWith("Missing required option: '--a=<a>'"),
      Seq("-b", "x", "--a", "x", "--c") -> failCommandLineParsingWith("Unknown option: '--c'"),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-h] --a=<a> -b=<b>
      --a=<a>
  -b, -B=<b>
  -h, --help    Show this help message and exit.
""")
    )
  }

  "parsing opt with the picocli builder directly specified through the impl specific extension test" should behave like {
    val spec =
      for {
        a <- opt[String]("--a").withPicocliOptionSpecBuilder(_.defaultValue("hello").paramLabel("<a>"))
        b <- opt[Int]("--b").default(1).withPicocliOptionSpecBuilder(_.paramLabel("<b>"))
        //      p <- param[String]("p").withPicocliOptionSpecBuilder(_.default("hello"))
      } yield IO {
        (a, b)
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith((Some("hello"), 1)),
      Seq("--a") -> failCommandLineParsingWith("Missing required parameter for option '--a' (<a>)"),
      Seq("--a", "hi") -> succeedWith((Some("hi"), 1)),
      Seq("--a", "hi", "--b", "2") -> succeedWith((Some("hi"), 2)),
      Seq("--c") -> failCommandLineParsingWith("Unknown option: '--c'")
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
      Seq() -> failSubcommandLineParsingWith("parsing failed for subcommand s"),
      Seq("--c", "hello") -> failCommandLineParsingWith("Unknown options: '--c', 'hello'"),
      Seq("--help") -> failWithUsageHelpRequested("""Main command with a subcommand
Usage: CommandLineParserTest [-h] [--b=PARAM] [COMMAND]
This is the main command.
      --b=PARAM
  -h, --help      Show this help message and exit.
Commands:
  s  A subcommand
"""),
      Seq("s", "--help") -> failWithUsageHelpRequested("""A subcommand
Usage: CommandLineParserTest s [-h] [--a=PARAM]
This is a subcommand.
      --a=PARAM
  -h, --help      Show this help message and exit.
""")
    )
  }

  "parsing command and subcommand properties test where header is specified on the subcommand" should behave like {
    val sub =
      subcommand("s").header("A subcommand").description("This is a subcommand.") {
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
          s <- sub
        } yield for {
          r <- s
        } yield (b, r)
      }

    runCommandLineTestCases(main)(
      Seq() -> failSubcommandLineParsingWith("parsing failed for subcommand s"),
      Seq("--c", "hello") -> failCommandLineParsingWith("Unknown options: '--c', 'hello'"),
      Seq("--help") -> failWithUsageHelpRequested("""Main command with a subcommand
Usage: CommandLineParserTest [-h] [--b=PARAM] [COMMAND]
This is the main command.
      --b=PARAM
  -h, --help      Show this help message and exit.
Commands:
  s  A subcommand
"""),
      Seq("s", "--help") -> failWithUsageHelpRequested("""A subcommand
Usage: CommandLineParserTest s [-h] [--a=PARAM]
This is a subcommand.
      --a=PARAM
  -h, --help      Show this help message and exit.
""")
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
      Seq() -> succeedWith("hello"),
      Seq("--help") -> failWithUsageHelpRequested("""Main command with no args
Usage: CommandLineParserTest [-h]
This is the main command.
  -h, --help   Show this help message and exit.
""")
    )
  }

  "accessing the full list of command line args along with options test" should behave like {
    val main =
      command.header("Main command with no args").description("This is the main command.") {
        for {
          a <- opt[Int]("--a").default(1)
          b <- param[String].label("xxxx")
          args <- args
        } yield IO {
          (a, b, args)
        }
      }

    runCommandLineTestCases(main)(
      Seq() -> succeedWith((1, None, List())),
      Seq("--a", "2", "hello") -> succeedWith((2, Some("hello"), List("--a", "2", "hello")))
    )
  }

  "opts of built-in types" should behave like {
    val spec =
      for {
        s <- opt[String]("s").default("")
        b <- opt[Boolean]("b").default(true)
        i <- opt[Int]("i").default(1)
        l <- opt[Long]("l").default(2)
        f <- opt[Float]("f").default(1.0f)
        d <- opt[Double]("d").default(1.0)
        g <- opt[Duration]("g").default(1.second)
        h <- opt[FiniteDuration]("k").default(2.seconds)
        j <- opt[BigDecimal]("j").default(BigDecimal(1.0))
      } yield IO {
        (s, b, i, l, f, d, g, h, j)
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith(("", true, 1, 2, 1.0f, 1.0, 1.second, 2.seconds, BigDecimal(1.0))),
      Seq("-s", "", "-i", "1", "-l", "2", "-f", "1.0f", "-d", "1.0", "-g", "1 second", "-k", "2 seconds", "-j", "1.0") -> succeedWith(("", true, 1, 2, 1.0f, 1.0, 1.second, 2.seconds, BigDecimal(1.0))),
      Seq("--help") -> failWithUsageHelpRequested("""Usage: CommandLineParserTest [-bh] [-d=PARAM] [-f=PARAM] [-g=PARAM] [-i=PARAM]
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
""")
    )
  }

  "params of built-in types" should behave like {
    val spec =
      for {
        s <- param[String].default("")
        b <- param[Boolean].default(true)
        i <- param[Int].default(1)
        l <- param[Long].default(2)
        f <- param[Float].default(3.0f)
        d <- param[Double].default(4.0)
        g <- param[Duration].default(1.second)
        h <- param[FiniteDuration].default(2.seconds)
        j <- param[BigDecimal].default(BigDecimal(1.0))
      } yield IO {
        (s, b, i, l, f, d, g, h, j)
      }

    runCommandLineTestCases(spec)(
      Seq("", "true", "1", "2", "1.0", "1.0", "1 second", "2 seconds", "3.0") -> succeedWith(("", true, 1, 2, 1.0f, 1.0, 1.second, 2.seconds, BigDecimal(3.0)))
    )
  }

  "turning off default help option" should behave like {
    val main =
      command.header("Main command").description("This is the main command.")
        .help(false).commandLine {
        for {
          a <- opt[Int]("-a").default(1)
          b <- param[String].label("xxxx")
          args <- args
        } yield IO {
          (a, b, args)
        }
      }

    runCommandLineTestCases(main)(
      Seq() -> succeedWith((1, None, List())),
      Seq("-a", "2", "hello") -> succeedWith((2, Some("hello"), List("-a", "2", "hello"))),
      Seq("--help") -> failCommandLineParsingWith("Unknown option: '--help'")
    )
  }

  "adding version help option" should behave like {
    val main =
      command.header("Main command").description("This is the main command.")
        .version("1.0").commandLine {
        for {
          a <- opt[Int]("-a").default(1)
          b <- param[String].label("xxxx")
          args <- args
        } yield IO {
          (a, b, args)
        }
      }

    runCommandLineTestCases(main)(
      Seq() -> succeedWith((1, None, List())),
      Seq("-a", "2", "hello") -> succeedWith((2, Some("hello"), List("-a", "2", "hello"))),
      Seq("-h") -> failWithUsageHelpRequested("""Main command
Usage: CommandLineParserTest [-hV] [-a=PARAM] xxxx
This is the main command.
      xxxx
  -a=PARAM        (default: 1).
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
"""),
      Seq("--version") -> failWithVersionHelpRequested("""1.0
""")
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
      Seq() -> succeedWith((None, None, None)),
      Seq("-c", "-v", "-f", "file.tar.gz") -> succeedWith((Some(true), Some(true), Some("file.tar.gz"))),
      Seq("-cvf", "file.tar.gz") -> succeedWith((Some(true), Some(true), Some("file.tar.gz")))
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
      Seq() -> succeedWith((None, None, None)),
      Seq("-c", "-v", "-f", "file.tar.gz") -> succeedWith((Some(true), Some(true), Some("file.tar.gz"))),
      Seq("-cvf", "file.tar.gz") -> failCommandLineParsingWith("Unknown options: '-cvf', 'file.tar.gz'")
    )
  }

  "automatic prefixes turned off" should behave like {
    val spec =
      command.prefixLongOptionsWith(None).prefixShortOptionsWith(None) {
        for {
          a <- opt[String]("a", "long")
        } yield IO {
          a
        }
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith(None),
      Seq("-a", "hello") -> failCommandLineParsingWith("Unmatched arguments from index 0: '-a', 'hello'"),
      Seq("a", "hello") -> succeedWith(Some("hello")),
      Seq("--long", "hello") -> failCommandLineParsingWith("Unmatched arguments from index 0: '--long', 'hello'"),
      Seq("long", "hello") -> succeedWith(Some("hello")),
      Seq("a") -> failCommandLineParsingWith("Missing required parameter for option 'long' (PARAM)"),
      Seq("-a") -> failCommandLineParsingWith("Unmatched argument at index 0: '-a'")
    )
  }

  "setting command name" should behave like {
    val sub =
      command.header("A subcommand.").description("This is a subcommand.") {
        for {
          a <- opt[String]("a")
        } yield IO {
          a
        }
      }

    val spec =
      command("main") {
        for {
          c <- opt[String]("c")
          s <- subcommand("s")(sub)
        } yield for {
          r <- s
        } yield (c, r)
      }

    runCommandLineTestCases(spec)(
      Seq("--help") -> failWithUsageHelpRequested("""Usage: main [-h] [-c=PARAM] [COMMAND]
  -c=PARAM
  -h, --help   Show this help message and exit.
Commands:
  s  A subcommand.
"""),
      Seq("s", "--help") -> failWithUsageHelpRequested("""A subcommand.
Usage: main s [-h] [-a=PARAM]
This is a subcommand.
  -a=PARAM
  -h, --help   Show this help message and exit.
""")
    )
  }

  "parsing custom argument types" should behave like {
    val now = LocalDate.now

    implicit val parser = new ArgumentValueParser[LocalDate] {
      def parse(value: String) =
        Try(LocalDate.parse(value)).toEither

      def empty: LocalDate =
        now
    }

    implicit val printer = new ArgumentValuePrinter[LocalDate] {
      def print(value: LocalDate) =
        value.toString
    }

    val spec =
      for {
        d <- opt[LocalDate]("date").default(now).description("The date")
      } yield IO {
        println(s"date: $d")
      }

    runCommandLineTestCases(spec)(
      Seq() -> succeedWith(now),
      Seq("--date", "2021-01-20") -> succeedWith(LocalDate.parse("2021-01-20")),
      Seq("--date", "2021") -> failCommandLineParsingWith("Invalid value for option '--date': cannot convert '2021' to LocalDate (java.time.format.DateTimeParseException: Text '2021' could not be parsed at index 4)"),
      Seq("-h") -> failWithUsageHelpRequested(s"""Usage: CommandLineParserTest [-h] [--date=PARAM]
      --date=PARAM   The date (default: ${now.toString}).
  -h, --help         Show this help message and exit.
""")
    )
  }

  // TODO: add test for Try and Either
}
