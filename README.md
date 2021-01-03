### Sclap: Scala Command Line Apps Made Simple

An example says more than a thousand words:

```scala
import io.jobial.sclap.CommandLineApp
import scala.concurrent.duration._

object PingExample extends CommandLineApp {

  def run =
    for {
      count <- opt("count", 10)
      timeout <- opt("timeout", 5 seconds)
      timeToLive <- opt[Int]("ttl")
      host <- param[String].required
    } yield
      myPing(host, count, timeout, timeToLive)

  def myPing(host: String, count: Int, timeout: FiniteDuration, timeToLive: Option[Int]) =
    println(s"Pinging $host with $count packets, $timeout timeout and $timeToLive ttl...")

}
```

which produces the following command line usage message when you run it with --help on the command line:

```text
> PingExample --help

Usage: <main class> [-h] [--count=PARAM] [--timeout=PARAM] [--ttl=PARAM] PARAM
      PARAM
      --count=PARAM     (default: 10).
  -h, --help            Show this help message and exit.
      --timeout=PARAM   (default: 5 seconds).
      --ttl=PARAM
```

If you run it without any arguments, you will get the following on the standard error along with a non-zero exit code,
as expected:

```text
> PingExample

Missing required parameter: 'PARAM'
Usage: <main class> [-h] [--count=PARAM] [--timeout=PARAM] [--ttl=PARAM] PARAM
      PARAM
      --count=PARAM     (default: 10).
  -h, --help            Show this help message and exit.
      --timeout=PARAM   (default: 5 seconds).
      --ttl=PARAM
```

If you run it with the argument "localhost", you should get:

```text
> PingExample localhost

Pinging localhost with 10 packets, timeout: 5 seconds, ttl: None...
```

Finally, if you provide one of the options, you will see something like:

```text
> PingExample --ttl=100 localhost

Pinging localhost with 10 packets, timeout: 5 seconds, ttl: Some(100)...
```

Of course, these examples assume that you have created an alias to or wrapped up your Scala app so that you can execute
it on the command line as `PingExample`).

#### Introduction

Sclap is a purely functional, type safe, composable, easy to test command line argument parser library for Scala.
Command line is still king and writing command line tools should be straightforward and painless. Although Sclap is
built on Cats, Cats Effect and Cats Free in a purely functional style and combines best with the Cats ecosystem, you
don't need to know any of those libraries to depend on it. Sclap can be used seamlessly in non-Cats based or non-FP
applications as well. It comes with:

* Automatic, fully customizable usage help generation
* Completely type safe access to command line options and parameters in your application code
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

#### A very basic example...

```scala
import io.jobial.sclap.CommandLineApp

object HelloExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("hello")
    } yield
      println(s"hello $hello")

}
```

which produces the following usage help:

...

You can find this example along with many other - more complex - ones at ....

To use Sclap you need to add

```scala
"io.jobial" % "sclap" %% "0.9.0"
```

to your build.sbt or

```xml

<dependency>
    <groupId>io.jobial</groupId>
    <artifactId>sclap_${scala.version}</artifactId>
    <version>0.9.0</version>
</dependency>
```

to pom.xml if you use Maven where scala.version is either 2.11, 2.12, 2.13 and 3.0 coming soon...

#### ...and a bit more detailed one

```scala
import io.jobial.sclap.CommandLineApp
import concurrent.duration._

object PingExample extends CommandLineApp {

  def run =
    for {
      count <- opt("count", 10)
      timeout <- opt("timeout", 5 seconds)
      timeToLive <- opt[Int]("ttl")
      host <- param[String].required
    } yield
      myPing(host, count, timeout, quiet)

}
```

which produces the usage:

...

A few things to note here:

* Sclap correctly infers the type of command line options and parameters. For example, `timeToLive` is an `Option[Int]`
  because it is not required to be specified by the caller. Host, on the other hand, is a `String` (not
  an `Option[String]`)
  because it is required. The same way, timeout is a `Duration` because it has a default value, so it is always
  available. Also, the type of timeout is inferred from the default value. By being type safe, there is virtually no
  possibility of ending up with options and parameters being in an "illegal state". You can be sure your opts and params
  are always valid in your application logic, otherwise Sclap will catch the problem before it reaches your code and
  handles the error appropriately (for example, it returns an error exit code and prints the error and usage messages).


* Sclap has built-in support for common argument value types (`String`, `Int`, `Double`, `Duration`, ...). You can
  easily add support for further types (or override the defaults) by implementing instances of the `ArgumentValueParser`
  and
  `ArgumentValuePrinter` type classes (see examples later).


* The app extends the `CommandLineApp` trait and has to implement the `run` function. The result of this function is of
  type `CommandLine[_]`. You don't really need to know much about it though: as long as you implement your `run`
  function in this format, it will return the right type and Sclap will be able to interpret your CLI.


* In the yield part of the for {...}, you can return pretty much anything, Sclap will know how to deal with it,
  including error handling. You might have noticed though that the return type of the yield block is actually always
  an `IO[_]`. If you are not familiar with the IO monad, all you need to know about it is that your application logic
  has to be enclosed in an IO ('lifted' into an IO context) before it is returned in the yield part of the for
  comprehension in the run function. If your application logic results in something other than an IO, it gets
  implicitly '
  lifted' into an `IO[_]` context. For example, if yield has code that returns an Int, it will implicitly
  become `IO[Int]`, taking care of any exceptions potentially thrown in the process. Also, if yield results in a Future
  or a Try, Sclap will know how to lift them into an `IO[_]` context in a safe
  (referentially transparent) way, propagating and handling errors automatically. The IO context guarantees that your
  application logic will only run once the arguments have been parsed and validated safely.


* Sclap will handle errors returned by your application code automatically: if your app throws an exception (or returns
  an error state in an `IO`, `Future` or `Try`), it will automatically be turned into a non-zero exit code.
  Alternatively, you can return an `IO[ExitStatus]` to explicitly specify the exit code (see `ExitStatus` in Cats
  Effect).

#### Anatomy

Here are a few pointers on the internal structure of a command line description in Sclap. An application typically
implements the `CommandLineApp` trait, which provides a safe implementation of the `main` function relying on Cats
Effect's `IOApp`. The app has to implement the

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

which is just a usual monadic expression using the `CommandLineArgSpec` Free monad mentioned above. The for part of the
for comprehension binds the options and parameters to names, and the yield returns the application logic. As mentioned
before, the return type of the yield part is always `IO[_]`. This is important: `IO` is pure and allows the library to
process the description safely, without any side-effects. To make it more convenient for applications that do not use
Cats Effect, Sclap provides safe implicits to lift other common return types (`Future`, `Try` or `Any`) into an IO
context in a referentially transparent way (of course, the rest of your code will not become referentially transparent).

Sclap does not rely on any macros.

#### Parameters and Options

#### Command header and description

#### Subcommands

#### What if I want to use Future?

#### How about returning Try?

#### What if my code throws Exceptions?

#### Custom type arguments

#### Accessing all arguments

#### Customizing the usage help format

#### Generating bash completion

#### Testing your app

Sclap comes with the `CommandLineAppTestHelper` trait to help you write tests against your CLI specs:

#### Further Examples

#### How does it work?

Sclap is modular and it has the following components:

* **sclap-core:** defines the DSL, built on cats-free and cats-effect; the DSL is implementation independent, leaving it
  open for alternative parser implementations and making it more future proof in case the default parser impl (which
  currently uses Picocli) becomes obsolete or unmaintained.
* **sclap-app:** defines the CommandLineApp trait and other helper functionality.
* **sclap-picocli:** the default Sclap parser implementation built on Picocli, which is a mature command line parsing
  library with a traditional, non-safe Java API. Fortunately it comes with no dependencies apart from the Java standard
  library and exposes a reusable API.
* **sclap-examples:** Example apps.

Sclap relies on the Free monad class in cats-free to implement the DSL to describe the command line interface. The DSL
is used in two passes: the first pass builds the command line interface structure, which is then used in a second pass
to parse the actual arguments passed to the app and bind the results to the values the monadic expressions, or to
generate the command line usage text in case of a failure or if --help is requested. The application logic is
represented as an IO monad, which comes from cats-effect. By describing the application logic in a referentially
transparent manner, it is possible to evaluate the command line description multiple times without any side effects (
like running actual application logic, for example).

#### Implicits

Sclap relies on a few carefully designed implicits to make the syntax more concise. If you want to override the defaults
or have an aversion to implicits, you can always choose to not include the built-in ones in your code by extending the
CommandLineAppNoImplicits trait instead and cherry-picking the implicits you need separately. If you decide not to use
any of the implicits provided by the library, the syntax becomes slightly more verbose but still manageable. Here is an
example:

#### Implementation dependent extensions

If you need to access the implementation specific features in Picocli for whatever reason, the sclap-picocli module
provides extensions to Opts and Params that allow you to access the underlying Builder instances directly:

```scala

```

This way you override or customize all aspects of the command line description if needed.