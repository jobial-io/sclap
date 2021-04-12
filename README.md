# Sclap: Scala Command Line Apps Made Simple

An example speaks more than a thousand words:

```scala
import io.jobial.sclap.CommandLineApp
import scala.concurrent.duration._
import cats.effect.IO

object PingExample extends CommandLineApp {

  def run =
    for {
      count <- opt[Int]("count").default(10).description("Number of packets")
      timeout <- opt[Duration]("timeout").default(5.seconds).description("The timeout")
      timeToLive <- opt[Int]("ttl").description("Time to live")
      host <- param[String].label("<hostname>").description("The host").required
    } yield
      myPing(host, count, timeout, timeToLive)

  def myPing(host: String, count: Int, timeout: Duration, timeToLive: Option[Int]) =
    IO(println(s"Pinging $host with $count packets, $timeout timeout and $timeToLive ttl..."))

}
```

which produces the following command line usage message when run with --help:

```text
> PingExample --help

Usage: PingExample [-h] [--count=PARAM] [--timeout=PARAM] [--ttl=PARAM] <hostname>
      <hostname>        The hostname.
      --count=PARAM     Number of packets (default: 10).
  -h, --help            Show this help message and exit.
      --timeout=PARAM   The timeout (default: 5 seconds).
      --ttl=PARAM       Time to live.
```

On a colour terminal you should get something like:

![alt PingExample](https://raw.githubusercontent.com/jobial-io/sclap/master/sclap-examples/pingExampleScreenshot.png)

If you run it without any arguments, you will get the following on the standard error along with a non-zero exit code,
as expected:

```text
> PingExample

Missing required parameter: '<hostname>'
Usage: PingExample [-h] [--count=PARAM] [--timeout=PARAM] [--ttl=PARAM] <hostname>
      <hostname>        The hostname.
      --count=PARAM     Number of packets (default: 10).
  -h, --help            Show this help message and exit.
      --timeout=PARAM   The timeout (default: 5 seconds).
      --ttl=PARAM       Time to live.
```

If you run it with the argument "localhost", you should get:

```text
> PingExample localhost

Pinging localhost with 10 packets, timeout: 5 seconds, ttl: None...
```

Finally, if you specify some options, you will see something like:

```text
> PingExample --count=2 --ttl=100 localhost

Pinging localhost with 2 packets, timeout: 5 seconds, ttl: Some(100)...
```

Of course, these examples assume that you have created an alias to your Scala app or wrapped it up in a script so that
you can execute it on the command line as `PingExample`.

A few things to note here:

* **Type safety**: Sclap correctly infers the type of each command line option and parameter. For example, `timeToLive` is
  an `Option[Int]`
  because it is not required to be specified by the caller. Host, on the other hand, is a `String` (not
  an `Option[String]`)
  because it is required. The same way, timeout is a `Duration` because it has a default value, so it is always
  available. By being type safe, there is virtually no possibility of ending up with options and parameters being in
  an "illegal state". You can be sure your opts and params are always valid and available in your application logic,
  otherwise Sclap will catch the problem before it reaches your code and handles the error appropriately (for example,
  it returns an error exit code and prints the error and usage messages).


* **Custom type support**: Sclap has built-in support for common argument value types (`String`, `Int`, `Double`
  , `Duration`, ...). You can easily add support for further types (or override the defaults) by implementing instances
  of the `ArgumentValueParser`
  and `ArgumentValuePrinter` type classes (see examples later).


* The app extends the `CommandLineApp` trait and has to implement the `run` function. The result of this function is of
  type `CommandLine[_]`. You don't really need to know much about it though: as long as you implement your `run`
  function in this format, it will return the right type and Sclap will be able to interpret your CLI.


* In the yield part of the for {...}, you can return pretty much anything, Sclap will know how to deal with it,
  including error handling. You might have noticed though that the return type of the yield block is actually always
  an `IO[_]`. If you are not familiar with the IO monad, all you need to know about it is that your application logic
  has to be enclosed in an IO ('lifted' into an IO context) before it is returned in the yield part of the for
  comprehension in the run function. If your application logic results in something other than an IO, it gets
  implicitly 'lifted' into an `IO[_]` context. For example, if yield has code that returns an Int, it will implicitly
  become `IO[Int]`, taking care of any exceptions potentially thrown in the process. Also, if yield results in
  a `Future`, `Try` or an `Either[Throwable, _]`, Sclap will know how to lift them into an `IO[_]` context in a safe
  (referentially transparent) way, propagating and handling errors automatically. The IO context guarantees that your
  application logic will only run once the arguments have been parsed and validated safely.


* Sclap will handle errors returned by your application code automatically: if your app throws an exception (or returns
  an error state in an `IO`, `Future` or `Try`), it will automatically be turned into a non-zero exit code.
  Alternatively, you can return an `IO[ExitCode]` to explicitly specify the exit code (see `cats.effect.ExitCode` in
  Cats Effect).

## Introduction

**Sclap** is a purely functional, type safe, composable, easy to test command line argument parser for Scala. Command
line is still king and writing command line tools should be straightforward and painless.

Although Sclap is built on
**Cats**, **Cats Effect** and **Cats Free** in a purely functional style and combines best with the Cats ecosystem, you
don't need to know any of those libraries to use it. Sclap can be used seamlessly in non-Cats based or non-FP
applications as well. It comes with:

* Automatic, fully customizable usage help generation
* Type safe access to command line options and parameters in your application code
* Support for custom type arguments
* ANSI colours
* Bash and ZSH autocomplete script generation
* Subcommand support, with support for composable, nested, hierarchic interfaces
* Straightforward, composable, purely functional, referentially transparent CLI description
* Extendable API and implementation, with parser implementation decoupled from DSL
* Flag, single-value and multiple value options
* POSIX-style short option names (-a) with grouping (-abc)
* GNU-style long option names (--opt, --opt=val)

The motivation is to help promote Scala as an alternative to implementing command line tools as scripts and to make it
easier to expose existing functionality on the command line. Writing CLI tools should be a very simple exercise and
Scala today is a better language for the task than most others (including scripting languages). Sclap aims to provide a
well maintained and stable library that is feature rich enough to cover all the modern requirements.

### A very basic example...

```scala
import io.jobial.sclap.CommandLineApp
import cats.effect.IO

object HelloExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("hello")
    } yield
      IO(println(s"hello $hello"))

}
```

which produces the following usage help:

```
> HelloExample --help

Usage: HelloExample [-h] [--hello=PARAM]
  -h, --help          Show this help message and exit.
      --hello=PARAM
```

or

```
> HelloExample --hello=world

hello Some(world)
```

You can find this example along with many other more complex
ones [here](https://github.com/jobial-io/sclap/tree/master/sclap-examples/src/main/scala/io/jobial/sclap/example).

To use Sclap you need to add

```scala
libraryDependencies ++= Seq(
  "io.jobial" %% "sclap" % "1.1.1"
)
```

to your `build.sbt` or

```xml

<dependency>
    <groupId>io.jobial</groupId>
    <artifactId>sclap_${scala.version}</artifactId>
    <version>1.1.1</version>
</dependency>
```

to `pom.xml` if you use Maven, where scala.version is either 2.11, 2.12, 2.13 and 3.0 coming soon...

### ...and a more detailed one

```scala
...
```

which produces the usage:

...

## Positional parameters and options

## Command header and description

```scala
import io.jobial.sclap.CommandLineApp
import cats.effect.IO

object HelloWorldExample extends CommandLineApp {

  def run =
    command.header("Hello World")
      .description("A hello world app with one option.") {
        for {
          hello <- opt[String]("hello").default("world")
        } yield
          IO(println(s"hello $hello"))
      }
}
```

which produces the usage help:

```
> HelloWorldExample --help

Hello World
Usage: HelloWorldExample [-h] [--hello=PARAM]
A hello world app with one option.
  -h, --help          Show this help message and exit.
      --hello=PARAM   (default: world).
```

## Subcommands

Sclap supports subcommands naturally by nesting command specs. Let's say we want to define a command line interface to
add or subtract two numbers:

```scala

import io.jobial.sclap.CommandLineApp
import cats.effect.IO

object ArithmeticExample extends CommandLineApp {

  def add =
    for {
      a <- param[Int].required
      b <- param[Int].required
    } yield IO(a + b)

  def sub =
    for {
      a <- param[Int].required
      b <- param[Int].required
    } yield IO(a - b)

  def run =
    for {
      addResult <- subcommand("add")(add)
      subResult <- subcommand("sub")(sub)
    } yield for {
      r <- addResult orElse subResult
    } yield IO(println(r))
}
```

```
> ArithmeticExample --help

Usage: ArithmeticExample [-h] [COMMAND]
  -h, --help   Show this help message and exit.
Commands:
  add
  sub
```

and

```
> ArithmeticExample add --help

Usage: ArithmeticExample add PARAM PARAM
  PARAM
  PARAM
```

so we can

```
> ArithmeticExample add 3 2
5
> ArithmeticExample sub 3 2
1

```

The structure of a subcommand is the same as of a main command. Commands can be arbitrarily combined into a hierarchy of
subcommands using the `subcommand(...)` function. Since everything is referentially transparent here, subcommand and
command definitions can be reused and combined arbitrarily, without any side-effect.

This can be demonstrated by improving on the arithmetic example the following way. Let's say we want to implement
division and multiplication in addition to the previous operations. Since all these require two operands and only differ
in the operator, it would be redundant to implement them as separate functions. Instead, the code for the subcommands
can be shared the following way, for example:

```scala

def operation[T: ArgumentValueParser](name: String, op: (T, T) => T) =
  subcommand[T](name) {
    for {
      a <- param[T].required
      b <- param[T].required
    } yield IO(op(a, b))
  }

def run =
  for {
    subcommandResult <- subcommands(
      operation[Double]("add", _ + _),
      operation[Double]("sub", _ - _),
      operation[Double]("mul", _ * _),
      operation[Double]("div", _ / _)
    )
  } yield subcommandResult.map(println)

```

As can be seen from this example, the code for the subcommands has been completely generified: both the operand type and
the operators are parameters here. Since the type is a parameter now, we need to make sure there is
an `ArgumentValueParser` instance available for it to be able to use it in `param[...]`. Another thing to note is that
Sclap provides a useful alternative to `orElse` when it comes to combining the results of the subcommands.
The `subcommands(...)` function takes a variable number of subcommand definitions as arguments, and returns the result
of the one selected by the caller (just like as if `orElse` was used between the individual subcommand results). This is
useful because the selection of the subcommand result can be moved to the main for comprehension, without introducing
another one in the yield section (see previous example).

We can now:

```
> ArithmeticExample mul 3 2
6
> ArithmeticExample div 3 2
1.5

```

To further improve our arithmetic app, we can add headers and description to the main command and the subcommands like
this:

```scala

def operation[T: ArgumentValueParser](name: String, op: (T, T) => T) =
  subcommand[T](name)
    .header(s"${name.capitalize} two numbers.")
    .description("Speficy the two operands and the result will be printed.") {
      for {
        a <- param[T].description("The first operand.").required
        b <- param[T].description("The second operand.").required
      } yield IO(op(a, b))
    }

def run =
  command("arithmetic")
    .header("Simple arithmetics on the command line.")
    .description("Use the following commands to add, subtract, multiply, divide numbers.") {
      for {
        subcommandResult <- subcommands(
          operation[Double]("add", _ + _),
          operation[Double]("sub", _ - _),
          operation[Double]("mul", _ * _),
          operation[Double]("div", _ / _)
        )
      } yield subcommandResult.map(println)
    }

```

## Error handling

As explained before, your `run` function (either explicitly or implicitly) always takes this format:

```scala
import cats.effect.IO

def run =
  for {
    _ <- opt(...)
  } yield IO {
    // app code
  }
```

If the IO results in an error state, the default error handling is to print the error message and return a non-zero exit
code:

```scala
import cats.effect.IO
import io.jobial.sclap.CommandLineApp

object ErrorExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("hello").default("world")
    } yield
      IO.raiseError(new RuntimeException("an error occurred..."))

}
```

```
> ErrorExample
an error occurred...
```

### What if my code throws Exceptions?

Since the application code in yield is always wrapped in an IO, an exception will result in an IO with an error state
exactly the same way as above:

```scala
import io.jobial.sclap.CommandLineApp
import cats.effect.IO

object ExceptionExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("hello").default("world")
    } yield IO {
      throw new RuntimeException("an error occurred...")
    }

}
```

should execute like

```
> ExceptionExample
an error occurred...
```

with a non-zero exit code. However, if you call it with --help, the application code will never run and the exception
doesn't get thrown, as you would expect:

```
> ExceptionExample --help
Usage: ErrorExample [-h] [--hello=PARAM]
  -h, --help          Show this help message and exit.
      --hello=PARAM
```

### What if I want to use Future?

You can return a Future seamlessly in the yield part of the run function:

```scala
import concurrent.Future

def run =
  for {
    hello <- opt[String]("hello").default("world")
  } yield Future {
    println(s"hello $hello")
  }
```

It should produce:

```
> HelloExample

hello world
```

Of course, the `Future` gets executed only if Sclap could parse the arguments successfully and help is not requested.

Errors are handled as expected:

```scala
import concurrent.Future

def run =
  for {
    hello <- opt[String]("hello").default("world")
  } yield Future {
    throw new RuntimeException("there was an error...")
  }
```

should run like

```
> HelloExample
there was an error...
```

with a non-zero exit code.

### How about returning a Try or an Either?

You can return a Try or an Either as well:

```scala
import util.Try

def run =
  for {
    hello <- opt[String]("hello").default("world")
  } yield Try {
    println(s"hello $hello")
  }
```

```scala
import util.Either

def run =
  for {
    hello <- opt[String]("hello")
  } yield hello match {
    case Some(hello) =>
      Right(hello)
    case None =>
      Left(new IllegalArgumentException("wrong argument"))
  }
```

The behaviour is the same as for Future.

## Custom type arguments

Argument values are handled type safely in Sclap. Parsing and printing arguments of different types are done through
the `ArgumentValueParser` and `ArgumentValuePrinter` type classes.

An example for parsing a command line option of type LocalDate:

```scala
import io.jobial.sclap.CommandLineApp
import io.jobial.sclap.core.{ArgumentValueParser, ArgumentValuePrinter}
import java.time.LocalDate
import scala.util.Try
import cats.effect.IO

object DateExample extends CommandLineApp {

  implicit val parser = new ArgumentValueParser[LocalDate] {
    def parse(value: String) =
      Try(LocalDate.parse(value)).toEither

    def empty: LocalDate =
      LocalDate.now
  }

  implicit val printer = new ArgumentValuePrinter[LocalDate] {
    def print(value: LocalDate) =
      value.toString
  }

  def run =
    for {
      d <- opt[LocalDate]("date").default(LocalDate.now).description("The date")
    } yield
      IO(println(s"date: $d"))
}
```

Instead of defining the printer directly, it can be derived from a `Show` intance. So the following would also work to
print the default argument value:

```scala
  implicit val localDateShow = Show.fromToString[LocalDate]
```

## Accessing the full argument list

If for some reason you need to access all the arguments as they were passed on the command line, you can use the args
function:

```scala
def run =
  for {
    a <- args
  } yield
    IO(println(a)) // a is a Seq[String] with all the arguments
```

## Customizing the usage help format

### Overriding the app name

The name of the app printed in the usage help is derived from the main class name by default. It can be overridden by
using either the syntax

```scala

def run =
  command(name = "my-app").description("My really cool app.") {
    for {
      o <- opt(...)
    } yield
    ...
  }
```

or by setting the `app.name` system property:

```
java -Dapp.name=my-app ...
```

## Generating a Bash or ZSH autocomplete script

...

## Testing your app

Sclap comes with the `CommandLineAppTestHelper` trait to help you write tests against your CLI specs:

## List of examples

You can find more
examples [here](https://github.com/jobial-io/sclap/tree/master/sclap-examples/src/main/scala/io/jobial/sclap/example).

## How does it work?

### Anatomy

A few pointers on the structure of the command line description in Sclap. An application typically implements
the `CommandLineApp` trait, which provides a safe implementation of the `main` function relying on Cats Effect's `IOApp`
. The app has to implement the

```scala
def run: CommandLine[Any]
```

function, which expands to

```scala
def run: CommandLineArgSpec[IO[Any]]
```

which in turn is the same as

```scala
def run: Free[CommandLineArgSpecA, IO[Any]]
```

The typical structure of a `run` function is

```scala
def run =
  for {
    o <- opt(...)
    ...
  } yield IO {
    ...
  }
```

which is just a monadic expression using the `CommandLineArgSpec` Free monad mentioned above. The for part of the
comprehension binds the option and parameter values to names, and the yield section returns the application logic. As
mentioned before, the return type of the yield part is always `IO[_]`. This is important: `IO` is pure and allows the
library to process the description safely, without any side-effects. To make it more convenient for applications that do
not use Cats Effect, Sclap provides safe implicits to lift other common return types (`Future`, `Try` or `Either`) into
an IO context in a referentially transparent way (of course, the rest of your code will not become referentially
transparent).

No macros used or animals harmed in the making of Sclap.

### Modules

Sclap is modular and comes with the following artifacts:

* **sclap-core:** defines the DSL, built on cats-free and cats-effect; the DSL is implementation independent, leaving it
  open for alternative parser implementations and making it future proof in case the default parser impl (which
  currently uses Picocli) becomes obsolete or unmaintained. An implementor has to implement the `executeCommandLine`
  function which takes the `CommandLine` description along with the command line args as arguments.
* **sclap-app:** provides the `CommandLineApp` trait and other helper functionality.
* **sclap-picocli:** the default Sclap parser implementation built on Picocli, which is a mature command line parsing
  library with a traditional, non-safe Java API. Fortunately it comes with no dependencies apart from the Java standard
  library and exposes a fairly reusable API.
* **sclap-examples:** Example apps.

Sclap relies on the Free monad class from cats-free to implement the DSL that describes the command line interface. The
DSL is used in two phases: the first pass builds the command line interface structure, which is then used in a second
pass to parse the actual arguments passed to the app and bind the results to the values in the monadic expression, or to
generate the command line usage text in case of a failure or if --help is requested. The application logic is
represented as an IO monad, which comes from cats-effect. By describing the application logic in a referentially
transparent manner, it is possible to evaluate the command line description multiple times without any side effects (
like running actual application logic, for example).

### Implicits

Sclap relies on a few carefully designed implicits to make the syntax more concise. If you want to override the defaults
or have an aversion to implicits, you can always choose to not include the built-in implicits in your code by extending the
`CommandLineAppNoImplicits` trait instead and cherry-pick the ones you need separately. If you decide not to use
any of the implicits provided by the library, the syntax becomes slightly more verbose but still manageable. Here is an
example:

...

## Implementation dependent extensions

If you need to access the implementation specific features in Picocli for whatever reason, the sclap-picocli module
provides extensions to Opts and Params that allow access to the underlying Builder instances directly, for example:

```scala

opt(...).withPicocliBuilder(_.hidden(true))
```

