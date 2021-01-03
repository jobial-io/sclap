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
package io.jobial.sclap.impl.picocli

import cats.data.State
import cats.effect.IO
import cats.free._
import cats.implicits._
import cats.{Id, ~>, _}
import io.jobial.sclap.core.{ArgumentValueParser, Logging, UsageHelpRequested, _}
import picocli.{CommandLine => PicocliCommandLine}
import picocli.CommandLine.Model.{OptionSpec, PositionalParamSpec, CommandSpec => PicocliCommandSpec}
import picocli.CommandLine.{DefaultExceptionHandler, IExceptionHandler2, IParseResultHandler2, ITypeConverter, ParseResult}

import java.io.PrintStream
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import io.jobial.sclap.core.implicits._

/**
 * An implementation of the Sclap command line parser using Picocli.
 */
trait PicocliCommandLineParser {
  this: CommandLineParserDsl with Logging =>

  val sclapLoggingEnabled = false

  private def debug(message: => String) =
    if (sclapLoggingEnabled)
      logger.debug(message)

  def parse[A](commandLine: CommandLine[A], args: Seq[String]) = {
    val result = commandLine.foldMap(parserCompiler(args)).run(CommandLineParsingContext().updateCommand(command))

    result.value._1
  }

  case class CommandLineParsingContext(command: Command = Command(), picocliCommandSpec: PicocliCommandSpec = PicocliCommandSpec.create) {

    def updateCommand(command: Command) = {
      picocliCommandSpec.options.asScala.filter(o => o.names.head == "-h" || o.names.head == "--help").map(picocliCommandSpec.remove)
      picocliCommandSpec.options.asScala.filter(o => o.names.head == "-V" || o.names.head == "--version").map(picocliCommandSpec.remove)
      picocliCommandSpec.mixinStandardHelpOptions(true)
      new PicocliCommandLine(picocliCommandSpec)
      picocliCommandSpec.commandLine().setPosixClusteredShortOptionsAllowed(command.clusteredShortOptionsAllowed)

      command.version match {
        case Some(version) =>
          picocliCommandSpec.version(version)
        case None =>
          picocliCommandSpec.options.asScala.filter(o => o.names.head == "-V" || o.names.head == "--version").map(picocliCommandSpec.remove)
      }

      if (!command.help)
        picocliCommandSpec.options.asScala.filter(o => o.names.head == "-h" || o.names.head == "--help").map(picocliCommandSpec.remove)

      copy(command = command)
    }

    def updateSpec(update: PicocliCommandSpec => PicocliCommandSpec) =
      copy(picocliCommandSpec = update(picocliCommandSpec))
  }

  type CommandLineParsingState[A] = State[CommandLineParsingContext, A]

  def typeConverterFor[T: ArgumentValueParser] =
    new PicocliCommandLine.ITypeConverter[T] {
      override def convert(s: String) = ArgumentValueParser[T].parse(s)
    }

  def normalize(name: String, command: Command) =
    if (name.size == 1) // short name
      command.prefixShortOptionsWith match {
        case Some(prefix) =>
          prefix + name
        case None =>
          name
      }
    else
      command.prefixLongOptionsWith match {
        case Some(prefix) =>
          if (name.startsWith(prefix) || command.prefixShortOptionsWith.map(prefix => name.startsWith(prefix)).getOrElse(false))
            name
          else
            prefix + name
        case None =>
          name
      }

  def parserCompiler(args: Seq[String]): CommandLineArgSpecA ~> CommandLineParsingState = new (CommandLineArgSpecA ~> CommandLineParsingState) {

    def apply[A](fa: CommandLineArgSpecA[A]): CommandLineParsingState[A] = {

      def optionSpecBuilder(names: Seq[String]) =
        OptionSpec.builder(names.toArray)

      def optionSpecBuilderForOpt[T: ArgumentValueParser](opt: OptSpec[_, T]) = {
        val specBuilder = optionSpecBuilder(opt.name +: opt.aliases)
          .`type`(ArgumentValueParser[T].resultClass).converters(typeConverterFor[T])
        opt.paramLabel.map(l => specBuilder.paramLabel(l)).getOrElse(specBuilder)
      }

      def addOptionSpec[T](specBuilder: OptionSpec.Builder, empty: T) = {
        State.modify[CommandLineParsingContext] { ctx =>
          specBuilder.names(specBuilder.names.map(normalize(_,  ctx.command)): _*)

          for {
            defaultValue <- Option(specBuilder.defaultValue)
          } yield
            if (ctx.command.printOptionDefaultValues) {
              val defaultValueString = s"(default: $defaultValue)"
              specBuilder.description(Option(specBuilder.description).getOrElse(Array()).lastOption
                .map(_ + defaultValueString).getOrElse(defaultValueString))
            }

          def addDot(s: String) = if (s.endsWith(".")) s else s"$s."

          for {
            description <- Option(specBuilder.description)
          } yield
            if (ctx.command.addDotToDescriptions) {
              specBuilder.description((addDot(description.reverse.head) :: description.reverse.toList.tail).reverse: _*)
            }
          val spec = specBuilder.build
          debug(s"adding $spec")
          ctx.updateSpec(_.addOption(spec))
        }.inspect(_ => empty)
      }

      def addOpt[T](opt: Opt[T], builder: OptionSpec.Builder => OptionSpec.Builder = identity[OptionSpec.Builder]) =
        addOptionSpec(builder(optionSpecBuilderForOpt(opt)(opt.parser)), Option(opt.parser.empty))

      def addOptWithDefaultValue[T](opt: OptWithDefaultValue[T], builder: OptionSpec.Builder => OptionSpec.Builder = identity[OptionSpec.Builder]) =
        addOptionSpec(builder(optionSpecBuilderForOpt(opt)(opt.parser).defaultValue(opt.defaultValuePrinter.print(opt.defaultValue))), opt.parser.empty)

      def addOptWithRequiredValue[T](opt: OptWithRequiredValue[T], builder: OptionSpec.Builder => OptionSpec.Builder = identity[OptionSpec.Builder]) =
        addOptionSpec(builder(optionSpecBuilderForOpt(opt)(opt.parser).required(true)), opt.parser.empty)

      def addParamSpec[T](specBuilder: PositionalParamSpec.Builder, empty: T) = {
        State.modify[CommandLineParsingContext] { ctx =>
          ctx.updateSpec { picocliCommandSpec =>
            val paramNum = ctx.picocliCommandSpec.positionalParameters().size
            if (specBuilder.index.isUnspecified) {
              specBuilder.descriptionKey(s"${specBuilder.descriptionKey}_${paramNum}")
              specBuilder.index(paramNum.toString)
            }
            val spec = specBuilder.build
            debug(s"adding $spec")
            ctx.picocliCommandSpec.addPositional(spec)
          }
        }.inspect(_ => empty)
      }

      def paramSpecBuilder[T: ArgumentValueParser](param: ParamSpec[_, T]) = {
        val specBuilder = PositionalParamSpec.builder
          .`type`(ArgumentValueParser[T].resultClass).converters(typeConverterFor[T])
          .descriptionKey(param.toString).required(false)
        param.paramLabel.map(l => specBuilder.paramLabel(l)).getOrElse(specBuilder)
        param.description.map(l => specBuilder.description(l)).getOrElse(specBuilder)
      }

      def addParam[T](param: Param[T], builder: PositionalParamSpec.Builder => PositionalParamSpec.Builder = identity[PositionalParamSpec.Builder]) =
        addParamSpec(builder(paramSpecBuilder(param)(param.parser).required(false).index(param.index.map(_.toString).getOrElse(""))), Option(param.parser.empty))

      def addParamWithDefaultValue[T](param: ParamWithDefaultValue[T], builder: PositionalParamSpec.Builder => PositionalParamSpec.Builder = identity[PositionalParamSpec.Builder]) =
        addParamSpec(builder(paramSpecBuilder(param)(param.parser).required(false).index(param.index.map(_.toString).getOrElse("")).defaultValue(param.defaultValuePrinter.print(param.defaultValue))), param.parser.empty)

      def addParamWithRequiredValue[T](param: ParamWithRequiredValue[T], builder: PositionalParamSpec.Builder => PositionalParamSpec.Builder = identity[PositionalParamSpec.Builder]) =
        addParamSpec(builder(paramSpecBuilder(param)(param.parser).required(true).index(param.index.map(_.toString).getOrElse(""))), param.parser.empty)

      fa match {
        case opt: Opt[A] =>
          addOpt(opt)
        case opt: OptWithDefaultValue[A] =>
          addOptWithDefaultValue(opt)
        case opt: OptWithRequiredValue[A] =>
          addOptWithRequiredValue(opt)
        case param: Param[A] =>
          addParam(param)
        case param: ParamWithDefaultValue[A] =>
          addParamWithDefaultValue(param)
        case param: ParamWithRequiredValue[A] =>
          addParamWithRequiredValue(param, _.required(true))
        case PicocliOpt(opt, builder) =>
          addOpt(opt, builder)
        case PicocliOptWithDefaultValue(opt, builder) =>
          addOptWithDefaultValue(opt, builder)
        case PicocliOptWithRequiredValue(opt, builder) =>
          addOptWithRequiredValue(opt, builder)
        case CommandWithCommandLine(command, _) =>
          State
            .modify[CommandLineParsingContext] { ctx =>
              ctx.updateCommand(command)
            }
            .modify(_.updateSpec { spec =>
              if (!Option(spec.name).isDefined)
                spec.name(command.name)
              //spec.remove(spec.options())
              command.header.map(spec.usageMessage.header(_))
              command.description.map(spec.usageMessage.description(_))
              spec
            })
            .inspect(_ => IO())
        case SubcommandWithCommandLine(subcommand, subcommandLine) =>
          val r = parse(subcommandLine, args).picocliCommandSpec
          val picocliSubcommandLine = new PicocliCommandLine(r)
          subcommand.aliases.map(aliases => picocliSubcommandLine.getCommandSpec.aliases(aliases: _*))
          State.modify[CommandLineParsingContext](_.updateSpec(_.addSubcommand(subcommand.name, picocliSubcommandLine))).inspect(_ => IO())
        case NoSpec(result) =>
          State.inspect(_ => result)
        case Args() =>
          State.inspect(_ => args.toList)
      }
    }
  }

  case class CommandLineExecutionContext(command: Command, paramCounter: Int = 0) {

    def incrementParamCounter(key: String) =
      copy(paramCounter = paramCounter + 1)
  }

  type CommandLineExecutionState[A] = State[CommandLineExecutionContext, A]

  class Handler extends PicocliCommandLine.AbstractParseResultHandler[Try[ParseResult]] {
    override protected def self: Handler = this

    override def handleParseResult(parseResult: ParseResult): Try[ParseResult] = {
      super.handleParseResult(parseResult)

      if (parseResult.isUsageHelpRequested)
        Failure(UsageHelpRequested())
      else if (parseResult.isVersionHelpRequested)
        Failure(VersionHelpRequested())
      else
        Success(parseResult)
    }

    def handle(pr: ParseResult) =
      Success(pr)
  }

  class ExceptionHandler extends DefaultExceptionHandler[Try[_]] {
    override def handleParseException(ex: PicocliCommandLine.ParameterException, args: Array[String]) = {
      super.handleParseException(ex, args)
      Failure(ex)
    }
  }

  def executeCommandLine[A](
    commandLine: CommandLine[A],
    args: Seq[String]
  ): IO[A] =
    executeCommandLine(commandLine, args, System.out, System.err)

  def executeCommandLine[A](
    commandLine: CommandLine[A],
    args: Seq[String],
    picocliOut: PrintStream,
    picocliErr: PrintStream
  ): IO[A] =
    executeCommandLine(commandLine, parse(commandLine, args), args, picocliOut, picocliErr)

  def executeCommandLine[A](
    commandLine: CommandLine[A],
    context: CommandLineParsingContext,
    args: Seq[String],
    picocliOut: PrintStream,
    picocliErr: PrintStream,
    subcommand: Boolean = false
  ) =
    for {
      picocliCommandLine <- IO(
        new PicocliCommandLine(context.picocliCommandSpec)
      )
      _ <- if (!subcommand) IO.fromTry {
        picocliCommandLine.parseWithHandlers(
          new Handler().useOut(picocliOut).useErr(picocliErr).asInstanceOf[IParseResultHandler2[Try[Object]]],
          new ExceptionHandler().useOut(picocliOut).useErr(picocliErr).asInstanceOf[IExceptionHandler2[Try[Object]]],
          args: _ *).map { _ =>
          picocliCommandLine.getParseResult
        }.recoverWith {
          case t: UsageHelpRequested =>
            Failure(t)
          case t: VersionHelpRequested =>
            Failure(t)
          case t: Throwable =>
            Failure(CommandLineParsingFailed(t))
        }
      } else IO()
      r <- commandLine.foldMap(executionCompiler(args, context, picocliOut, picocliErr)).run(CommandLineExecutionContext(context.command)).value._2.handleErrorWith {
        case t: CommandLineParsingFailedForSubcommand =>
          if (subcommand)
            IO.raiseError(t)
          else {
            // If all subcommands failed, print usage message to std err and fail
            picocliErr.print(picocliCommandLine.getUsageMessage)
            IO.raiseError(t)
          }
        case t =>
          IO.raiseError(t)
      }
    } yield r

  def executionCompiler(
    args: Seq[String],
    context: CommandLineParsingContext,
    picocliOut: PrintStream,
    picocliErr: PrintStream
  ): CommandLineArgSpecA ~> CommandLineExecutionState =
    new (CommandLineArgSpecA ~> CommandLineExecutionState) {

      val commandSpec = context.picocliCommandSpec

      def apply[A](fa: CommandLineArgSpecA[A]): CommandLineExecutionState[A] =
        fa match {
          case NoSpec(result) =>
            debug(s"returning no args value $result")
            State.inspect(_ => result.asInstanceOf[IO[A]])
          case Args() =>
            State.inspect(_ => args.toList)
          case Opt(name, _, _, _) =>
            debug(s"getting option value $name")
            State.inspect { ctx =>
//              println(normalize(name, ctx.command))
//              println(ctx.command)
//              println(commandSpec.optionsMap)
              Option(commandSpec.optionsMap.get(normalize(name, ctx.command)).getValue[A])
            }
          case OptWithDefaultValue(name, _, _, _, _) =>
            debug(s"getting option value $name")
            State.inspect(ctx => commandSpec.optionsMap.get(normalize(name, ctx.command)).getValue[A])
          case OptWithRequiredValue(name, _, _, _) =>
            debug(s"getting option value $name")
            State.inspect(ctx => commandSpec.optionsMap.get(normalize(name, ctx.command)).getValue[A])
          case p @ Param(_, _, _) =>
            State.modify[CommandLineExecutionContext](_.incrementParamCounter(p.toString)).inspect { context =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${context.paramCounter - 1}"
                else p.toString
              Option(commandSpec.positionalParameters.asScala.find(_.descriptionKey == descriptionKey).get.getValue[A])
            }
          case p @ ParamWithDefaultValue(_, _, _, _) =>
            State.modify[CommandLineExecutionContext](_.incrementParamCounter(p.toString)).inspect { context =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${context.paramCounter - 1}"
                else p.toString
              commandSpec.positionalParameters.asScala.find(_.descriptionKey == descriptionKey).get.getValue[A]
            }
          case p @ ParamWithRequiredValue(_, _, _) =>
            State.modify[CommandLineExecutionContext](_.incrementParamCounter(p.toString)).inspect { context =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${context.paramCounter - 1}"
                else p.toString
              commandSpec.positionalParameters.asScala.find(_.descriptionKey == descriptionKey).get.getValue[A]
            }
          case p @ ParamRange(param, _, _, _, _) =>
            State.inspect(_ => commandSpec.positionalParameters.asScala.find(_.descriptionKey == System.identityHashCode(p).toString).get.getValue[A])
          case PicocliOpt(o, _) =>
            debug(s"getting option value ${o.name}")
            State.inspect(_ => Option(commandSpec.optionsMap.get(o.name).getValue[A]))
          case PicocliOptWithDefaultValue(o, _) =>
            debug(s"getting option value ${o.name}")
            State.inspect(_ => commandSpec.optionsMap.get(o.name).getValue[A])
          case PicocliOptWithRequiredValue(o, _) =>
            debug(s"getting option value ${o.name}")
            State.inspect(_ => commandSpec.optionsMap.get(o.name).getValue[A])
          case CommandWithCommandLine(commandLine, _) =>
            State.inspect(_ => IO())
          case SubcommandWithCommandLine(subcommand, subCommandLine) =>
            State.inspect(context => Option(commandSpec.subcommands.get(subcommand.name).getParseResult) match {
              case Some(parseResult) =>
                executeCommandLine(subCommandLine, CommandLineParsingContext(context.command, commandSpec.subcommands.get(subcommand.name).getCommandSpec), args, picocliOut, picocliErr, true)
              case None =>
                debug(s"parsing args failed for subcommand ${subcommand.name}, proceeding...")
                IO.raiseError(CommandLineParsingFailedForSubcommand(subcommand.name, new RuntimeException))
            })
        }
    }

}

case class PicocliOpt[T: ArgumentValueParser](opt: Opt[T], builder: OptionSpec.Builder => OptionSpec.Builder)
  extends CommandLineArgSpecWithArgA[Option[T], T]

case class PicocliOptWithDefaultValue[T: ArgumentValueParser](opt: OptWithDefaultValue[T], builder: OptionSpec.Builder => OptionSpec.Builder)
  extends CommandLineArgSpecWithArgA[T, T]()

case class PicocliOptWithRequiredValue[T: ArgumentValueParser](opt: OptWithRequiredValue[T], builder: OptionSpec.Builder => OptionSpec.Builder)
  extends CommandLineArgSpecWithArgA[T, T]()

