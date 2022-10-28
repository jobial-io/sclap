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
package io.jobial.sclap.core

import cats.effect.IO
import cats.free.Free._
import io.jobial.sclap.core.implicits.ioExtraOps

import scala.reflect.ClassTag


trait CommandLineParserDsl {
  this: Logging =>

  /**
   * Specifies a command line option with the given name. The option is optional: 
   * it can be omitted on the command line and the result is of type Option[T]. 
   *
   * @param name
   * @param parser The parser used for the option value.
   * @tparam T The type of the option value.
   * @return
   */
  def opt[T: ArgumentValueParser](name: String, aliases: String*) =
    Opt[T](name).aliases(aliases: _*)

  /**
   * A command line (positional) parameter.
   *
   * @tparam T
   * @return
   */
  def param[T: ArgumentValueParser] =
    Param[T]()

  /**
   * Adds an existing command as a subcommand in the current command line context. Commands
   * can be freely combined with each other using subcommand(...). Because of referential transparency and the hierarchic 
   * structure of subcommands, the same command definition can be reused or even serve both as a subcommand and as a 
   * main command in different contexts.
   *
   * @return
   */
  def subcommand[T](name: String, aliases: String*) =
    Subcommand[T](name).aliases(aliases: _*)

  /**
   * Takes a non-empty list of subcommands and returns the IO result of the first one that returns a non-error result
   * when processing the arguments. For example,
   *
   * for {
   * r <- subcommands(sub1, sub2, sub3)
   * } yield r 
   *
   * is equivalent to 
   *
   * for {
   * r1 <- sub1
   * r2 <- sub2
   * r3 <- sub3
   * } yield r1 orElse r2 orElse r3
   *
   * and so on. It is effectively a way to select the subcommand that is invoked on the command line.
   *
   * @param subcommand
   * @param subcommands
   * @return
   */
  def subcommands[A](subcommand: SubcommandWithCommandLine[A], subcommands: SubcommandWithCommandLine[_ <: A]*): CommandLine[A] =
    subcommands.foldLeft(subcommand.build) { case (previous, s) =>
      for {
        p <- previous
        q <- s.build
      } yield p orElse q
    }

  /**
   * Starting point for a command line interface specification. The Command instance describes details that apply to the whole command
   * like header and description, for example. A Command instance itself does not fully specify the CLI, a CommandLine 
   * has to be added to it to be able to function as a full command line spec. The CommandLine can be added by simply applying
   * the command to a CommandLine instance. Example:
   *
   * command.header("My command") {
   * for {
   * level <- opt("level", "prod") ....
   * } yield ... 
   * }
   * }
   *
   * @return
   */
  def command = Command()

  /**
   * Starting point for a command line interface specification. The Command instance describes details that apply to the whole command
   * like header and description, for example. A Command instance itself does not fully specify the CLI, a CommandLine 
   * has to be added to it to be able to function as a full command line spec. The CommandLine can be added by simply applying
   * the command to a CommandLine instance. Example:
   *
   * command.header("My command") {
   * for {
   * level <- opt("level", "prod") ....
   * } yield ... 
   * }
   * }
   *
   * @return
   */
  def command(name: String) = Command(name = Some(name))

  /**
   * Return the full list of arguments as it was passed on the command line. It is rarely needed and param (with a range) is 
   * usually preferred.
   *
   * @return
   */
  def args = Args()

  /**
   * Directly specify an IO result in the command line processing context. Lifts an IO into the CommandLineArgSpec 
   * monad context without any command line specification or argument processing taking place. Useful, for example, 
   * in the edge case when a command has no options yet but some header and description are already specified. An IO
   * can be implicitly lifted into the CommandLineArgSpec context if the Sclap implicits are imported.
   *
   * @param result
   * @tparam A
   */
  def noSpec[A](result: IO[A]) = NoSpec(result)

  def executeCommandLine[A](commandLine: CommandLine[A], args: Seq[String]): IO[A]
}


/**
 * Type class that provides the functionality to parse a command line argument of type String into a value of type T.
 *
 * @tparam T
 */
abstract class ArgumentValueParser[T: ClassTag] {

  /**
   * Parse String into type T.
   */
  def parse(value: String): Either[Throwable, T]

  /**
   * The value to use in the first parsing phase when the AST for the command line is being built. This value is never 
   * used by the application (only used internally), but has to be specified explicitly by the implementor for each type.
   */
  def empty: T

  def resultTag = implicitly[ClassTag[T]]

  def resultClass = resultTag.runtimeClass
}

object ArgumentValueParser {

  def apply[A](implicit instance: ArgumentValueParser[A]) = instance
}

abstract class ArgumentValuePrinter[T] {

  def print(value: T): String
}

object ArgumentValuePrinter {

  def apply[A](implicit instance: ArgumentValuePrinter[A]) = instance
}

trait CommandLineArgSpecA[A] {

  def build: CommandLineArgSpec[A] =
    liftF[CommandLineArgSpecA, A](this)
}

abstract class CommandLineArgSpecAWithValueParser[A, P: ArgumentValueParser] extends CommandLineArgSpecA[A] {

  val parser = ArgumentValueParser[P]
}

abstract class OptSpec[A, P: ArgumentValueParser] extends CommandLineArgSpecAWithValueParser[A, P] {

  def name: String

  def label: Option[String]

  def description: Option[String]

  def aliases: Seq[String]
}

case class Opt[T: ArgumentValueParser](
  name: String,
  label: Option[String] = None,
  description: Option[String] = None,
  aliases: Seq[String] = Seq()
) extends OptSpec[Option[T], T]() {

  def label(value: String): Opt[T] =
    copy(label = Some(value))

  def description(value: String): Opt[T] =
    copy(description = Some(value))

  def alias(value: String): Opt[T] =
    copy(aliases = Seq(value))

  def aliases(values: String*): Opt[T] =
    copy(aliases = values)

  def defaultValue(value: T)(implicit printer: ArgumentValuePrinter[T]) =
    OptWithDefaultValue[T](name, value, label, description, aliases)

  def default(value: T)(implicit printer: ArgumentValuePrinter[T]) =
    defaultValue(value)

  def required =
    OptWithRequiredValue(name, label, description, aliases)
}

case class OptWithDefaultValue[T: ArgumentValueParser : ArgumentValuePrinter](
  name: String,
  defaultValue: T,
  label: Option[String] = None,
  description: Option[String] = None,
  aliases: Seq[String] = Seq()
) extends OptSpec[T, T] {

  def defaultValue(value: T): OptWithDefaultValue[T] =
    copy(defaultValue = value)

  def default(value: T) =
    defaultValue(value)

  def label(value: String): OptWithDefaultValue[T] =
    copy(label = Some(value))

  def description(value: String): OptWithDefaultValue[T] =
    copy(description = Some(value))

  def alias(value: String): OptWithDefaultValue[T] =
    copy(aliases = Seq(value))

  def aliases(values: String*): OptWithDefaultValue[T] =
    copy(aliases = values)

  val defaultValuePrinter = ArgumentValuePrinter[T]
}

case class OptWithRequiredValue[T: ArgumentValueParser](
  name: String,
  label: Option[String] = None,
  description: Option[String] = None,
  aliases: Seq[String] = Seq()
) extends OptSpec[T, T] {

  def defaultValue(value: T)(implicit printer: ArgumentValuePrinter[T]) =
    OptWithDefaultValue[T](name, value, label, description)

  def default(value: T)(implicit printer: ArgumentValuePrinter[T]) =
    defaultValue(value)

  def label(value: String): OptWithRequiredValue[T] =
    copy(label = Some(value))

  def description(value: String): OptWithRequiredValue[T] =
    copy(description = Some(value))

  def alias(value: String): OptWithRequiredValue[T] =
    copy(aliases = Seq(value))

  def aliases(values: String*): OptWithRequiredValue[T] =
    copy(aliases = values)
}

abstract class ParamSpec[A, T: ArgumentValueParser] extends CommandLineArgSpecAWithValueParser[A, T] {

  def label: Option[String]

  def description: Option[String]
}

abstract class SingleParamSpec[A, T: ArgumentValueParser] extends ParamSpec[A, T] {

  def index: Option[Int]
}

abstract class MultiParamSpec[A, T: ArgumentValueParser] extends ParamSpec[A, T] {

  def fromIndex: Option[Int]

  def toIndex: Option[Int]
}

case class Param[T: ArgumentValueParser](
  label: Option[String] = None,
  description: Option[String] = None,
  index: Option[Int] = None
) extends SingleParamSpec[Option[T], T] {

  def index(value: Int): Param[T] =
    copy(index = Some(value))

  def description(value: String): Param[T] =
    copy(description = Some(value))

  def label(value: String): Param[T] =
    copy(label = Some(value))

  def required =
    ParamWithRequiredValue(label, description, index)

  def defaultValue(value: T)(implicit printer: ArgumentValuePrinter[T]) =
    ParamWithDefaultValue[T](value, label, description, index)

  def default(value: T)(implicit printer: ArgumentValuePrinter[T]) =
    defaultValue(value)
}

case class ParamWithDefaultValue[T: ArgumentValueParser : ArgumentValuePrinter](
  defaultValue: T,
  label: Option[String] = None,
  description: Option[String] = None,
  index: Option[Int] = None
) extends SingleParamSpec[T, T] {

  def index(value: Int): ParamWithDefaultValue[T] =
    copy(index = Some(value))

  def description(value: String): ParamWithDefaultValue[T] =
    copy(description = Some(value))

  def label(value: String): ParamWithDefaultValue[T] =
    copy(label = Some(value))

  val defaultValuePrinter = ArgumentValuePrinter[T]
}

case class ParamWithRequiredValue[T: ArgumentValueParser](
  label: Option[String] = None,
  description: Option[String] = None,
  index: Option[Int] = None
) extends SingleParamSpec[T, T] {

  def index(value: Int): ParamWithRequiredValue[T] =
    copy(index = Some(value))

  def description(value: String): ParamWithRequiredValue[T] =
    copy(description = Some(value))

  def label(value: String): ParamWithRequiredValue[T] =
    copy(label = Some(value))
}

case class ParamRange[T: ArgumentValueParser](
  label: Option[String] = None,
  description: Option[String] = None,
  required: Boolean,
  fromIndex: Option[Int] = None,
  toIndex: Option[Int] = None
) extends ParamSpec[List[T], T] {

  def fromIndex(value: Int): ParamRange[T] =
    copy(fromIndex = Some(value))

  def toIndex(value: Int): ParamRange[T] =
    copy(toIndex = Some(value))
}

case class NoSpec[A](result: IO[A]) extends CommandLineArgSpecA[IO[A]]

case class Subcommand[A](
  name: String,
  header: Option[String] = None,
  description: Option[String] = None,
  aliases: Seq[String] = Seq()
) {

  def name(value: String): Subcommand[A] =
    copy[A](name = value)

  def header(value: String): Subcommand[A] =
    copy(header = Some(value))

  def description(value: String): Subcommand[A] =
    copy(description = Some(value))

  def aliases(values: String*): Subcommand[A] =
    copy(aliases = values)

  def commandLine(commandLine: CommandLine[A]) =
    SubcommandWithCommandLine[A](this.asInstanceOf[Subcommand[A]], commandLine)

  def apply(commandLine: CommandLine[A]) =
    this.commandLine(commandLine)
}

case class SubcommandWithCommandLine[A](
  subcommand: Subcommand[A],
  commandLine: CommandLine[A]
) extends CommandLineArgSpecA[IO[A]] {

  def orElse[B](subcommand: SubcommandWithCommandLine[B]) =
    for {
      r <- build
      q <- subcommand.build
    } yield r orElse q
}

case class CommandLineArgSpecInSubcommand[A](commandLine: CommandLine[A])

case class Command(
  name: Option[String] = None,
  header: Option[String] = None,
  description: Option[String] = None,
  printOptionDefaultValues: Boolean = true,
  addDotToDescriptions: Boolean = true,
  help: Boolean = true,
  version: Option[String] = None,
  clusteredShortOptionsAllowed: Boolean = true,
  prefixLongOptionsWith: Option[String] = Some("--"),
  prefixShortOptionsWith: Option[String] = Some("-"),
  headerHeading: Option[String] = None,
  synopsisHeading: Option[String] = None,
  descriptionHeading: Option[String] = None,
  parameterListHeading: Option[String] = None,
  optionListHeading: Option[String] = None,
  commandListHeading: Option[String] = None,
  footerHeading: Option[String] = None,
  printStackTraceOnException: Boolean = false
) {

  def name(value: String): Command =
    copy(name = Some(value))

  def header(value: String): Command =
    copy(header = Some(value))

  def description(value: String): Command =
    copy(description = Some(value))

  def help(value: Boolean) =
    copy(help = value)

  def clusteredShortOptionsAllowed(value: Boolean): Command =
    copy(clusteredShortOptionsAllowed = value)

  def prefixLongOptionsWith(value: Option[String]): Command =
    copy(prefixLongOptionsWith = value)

  def prefixShortOptionsWith(value: Option[String]): Command =
    copy(prefixShortOptionsWith = value)

  def headerHeading(value: Option[String]): Command =
    copy(headerHeading = value)

  def synopsisHeading(value: Option[String]): Command =
    copy(synopsisHeading = value)

  def descriptionHeading(value: Option[String]): Command =
    copy(descriptionHeading = value)

  def parameterListHeading(value: Option[String]): Command =
    copy(parameterListHeading = value)

  def optionListHeading(value: Option[String]): Command =
    copy(optionListHeading = value)

  def commandListHeading(value: Option[String]): Command =
    copy(commandListHeading = value)

  def footerHeading(value: Option[String]): Command =
    copy(footerHeading = value)
    
  def printStackTraceOnException(value: Boolean): Command =
    copy(printStackTraceOnException = value)

  def version(value: String): Command =
    copy(version = Some(value))

  def commandLine[A](commandLine: CommandLine[A]) =
    CommandWithCommandLine(this, commandLine).build

  def apply[A](commandLine: CommandLine[A]) =
    this.commandLine(commandLine)
}

case class CommandWithCommandLine[A](
  command: Command,
  commandLine: CommandLine[A]
) extends CommandLineArgSpecA[IO[A]] {

  // Prepend this to the rest of the command line
  override def build =
    super.build.flatMap(_ => commandLine)
}

case class Args() extends CommandLineArgSpecA[List[String]]

/**
 * Used internally to signal command line parsing failure.
 *
 * @param cause
 */
case class CommandLineParsingFailed(cause: Throwable)
  extends IllegalStateException(cause.getMessage, cause)

/**
 * Used internally to signal command line parsing failure for a subcommand.
 *
 * @param name
 * @param cause
 */
case class CommandLineParsingFailedForSubcommand(name: String, cause: Throwable)
  extends IllegalStateException(s"parsing failed for subcommand $name", cause)

sealed abstract class HelpRequested extends IllegalStateException

/**
 * Used internally to signal usage help request (triggered by --help usually).
 */
case class UsageHelpRequested()
  extends HelpRequested

/**
 * Used internally to signal version help request (triggered by --version usually).
 */
case class VersionHelpRequested()
  extends HelpRequested

/**
 * Use this exception to signal an error in the command line usage. It is handled like any other exception, but it
 * also prints the usage help.
 */
case class IncorrectCommandLineUsage(message: String)
  extends IllegalStateException(message)

/**
 * Used internally to differentiate between error in subcommand and main command.
 *
 * @param cause
 */
case class IncorrectCommandLineUsageInSubcommand(cause: IncorrectCommandLineUsage)
  extends IllegalStateException(cause.getMessage)
