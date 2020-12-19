package io.jobial.sclap.impl.picocli

import cats.data.State
import cats.effect.IO
import cats.free._
import cats.implicits._
import cats.{Id, ~>, _}
import io.jobial.sclap.core.{ArgumentValueParser, Logging, UsageHelpRequested, _}
import org.joda.time._
import picocli.{CommandLine => PicocliCommandLine}
import picocli.CommandLine.Model.{OptionSpec, PositionalParamSpec, CommandSpec => PicocliCommandSpec}
import picocli.CommandLine.{DefaultExceptionHandler, IExceptionHandler2, IParseResultHandler2, ITypeConverter, ParseResult}

import java.io.PrintStream
import scala.collection.JavaConversions._
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import io.jobial.sclap.core.implicits._


trait PicocliCommandLineParser {
  this: CommandLineParserDsl with Logging =>

  val sclapLoggingEnabled = false

  private def debug(message: => String) =
    if (sclapLoggingEnabled)
      logger.debug(message)

  def parse[A](commandLine: CommandLine[A], args: Seq[String]) = {
    val result = commandLine.foldMap(parserCompiler(args)).run(CommandLineParsingContext(Command(), PicocliCommandSpec.create.mixinStandardHelpOptions(true)))

    result.value._1
  }

  case class CommandLineParsingContext(command: Command, picocliCommandSpec: PicocliCommandSpec) {

    def updateSpec(update: PicocliCommandSpec => PicocliCommandSpec) =
      copy(picocliCommandSpec = update(picocliCommandSpec))
  }

  type CommandLineParsingState[A] = State[CommandLineParsingContext, A]

  def typeConverterFor[T: ArgumentValueParser] =
    new PicocliCommandLine.ITypeConverter[T] {
      override def convert(s: String) = ArgumentValueParser[T].parse(s)
    }

  def parserCompiler(args: Seq[String]): CommandLineArgSpecA ~> CommandLineParsingState = new (CommandLineArgSpecA ~> CommandLineParsingState) {

    def apply[A](fa: CommandLineArgSpecA[A]): CommandLineParsingState[A] = {

      def optionSpecBuilder(names: String*) = OptionSpec.builder(names.toArray)

      def optionSpecBuilderForOpt[T: ArgumentValueParser](opt: OptSpec[_, T]) = {
        val specBuilder = optionSpecBuilder(s"--${opt.name}")
          .`type`(ArgumentValueParser[T].resultClass).converters(typeConverterFor[T])
        opt.paramLabel.map(l => specBuilder.paramLabel(l)).getOrElse(specBuilder)
      }

      def addOptionSpec[T](specBuilder: OptionSpec.Builder, empty: T) = {
        State.modify[CommandLineParsingContext] { ctx =>
          for {
            defaultValue <- Option(specBuilder.defaultValue)
          } yield
            if (ctx.command.printDefaultValues) {
              val defaultValueString = s"(default: $defaultValue)"
              specBuilder.description(Option(specBuilder.description).getOrElse(Array()).lastOption
                .map(_ + defaultValueString).getOrElse(defaultValueString))
            }

          def addDot(s: String) = if (s.endsWith(".")) s else s"$s."

          for {
            description <- Option(specBuilder.description).lastOption
          } yield
            if (ctx.command.addDotToDescriptions) {
              specBuilder.description((addDot(specBuilder.description.reverse.head) :: specBuilder.description.reverse.toList.tail).reverse: _*)
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

      def addParamWithDefaultValue[T: ArgumentValuePrinter](param: ParamWithDefaultValue[T], builder: PositionalParamSpec.Builder => PositionalParamSpec.Builder = identity[PositionalParamSpec.Builder]) =
        addParamSpec(builder(paramSpecBuilder(param)(param.parser).required(false).index(param.index.map(_.toString).getOrElse("")).defaultValue(ArgumentValuePrinter[T].print(param.defaultValue))), param.parser.empty)

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
          addParamWithDefaultValue(param)(param.defaultValuePrinter)
        case param: ParamWithRequiredValue[A] =>
          addParamWithRequiredValue(param, _.required(true))
        case PicocliOpt(opt, builder) =>
          addOpt(opt, builder)
        case PicocliOptWithDefaultValue(opt, builder) =>
          addOptWithDefaultValue(opt, builder)
        case PicocliOptWithRequiredValue(opt, builder) =>
          addOptWithRequiredValue(opt, builder)
        case CommandWithCommandLine(_, command) =>
          State
            .modify[CommandLineParsingContext] { ctx =>
              ctx.copy(command = command)
            }
            .modify(_.updateSpec { spec =>
              spec.name(command.name)
              command.header.map(spec.usageMessage.header(_))
              command.description.map(spec.usageMessage.description(_))
              spec
            })
            .inspect(_ => IO())
        case Subcommand(name, subcommandLine, aliases) =>
          val r = parse(subcommandLine, args).picocliCommandSpec
          val picocliSubcommandLine = new PicocliCommandLine(r)
          aliases.map(aliases => picocliSubcommandLine.getCommandSpec.aliases(aliases: _*))
          State.modify[CommandLineParsingContext](_.updateSpec(_.addSubcommand(name, picocliSubcommandLine))).inspect(_ => IO())
        case NoSpec(result) =>
          State.inspect(_ => result)
        case Args() =>
          State.inspect(_ => args.toList)
      }
    }
  }

  case class ParamCounter(counter: Int = 0) {

    def increment(key: String) =
      copy(counter + 1)
  }

  type CommandLineExecutionState[A] = State[ParamCounter, A]

  class Handler extends PicocliCommandLine.AbstractParseResultHandler[Try[ParseResult]] {
    override protected def self: Handler = this

    override def handleParseResult(parseResult: ParseResult): Try[ParseResult] = {
      super.handleParseResult(parseResult)

      if (parseResult.isUsageHelpRequested)
        Failure(UsageHelpRequested())
      else
        Success(parseResult)
    }

    def handle(pr: ParseResult) =
      Success(pr)
  }

  class ExceptionHandler extends DefaultExceptionHandler[Try[PicocliCommandLine.ParameterException]] {
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
    executeCommandLine(commandLine, parse(commandLine, args).picocliCommandSpec, args, picocliOut, picocliErr)

  def executeCommandLine[A](
    commandLine: CommandLine[A],
    picocliCommandSpec: PicocliCommandSpec,
    args: Seq[String],
    picocliOut: PrintStream,
    picocliErr: PrintStream,
    subcommand: Boolean = false
  ) =
    for {
      picocliCommandLine <- IO(
        new PicocliCommandLine(picocliCommandSpec)
      )
      _ <- if (!subcommand) IO.fromTry {
        picocliCommandLine.parseWithHandlers(
          new Handler().useOut(picocliOut).useErr(picocliErr).asInstanceOf[IParseResultHandler2[Try[Object]]],
          new ExceptionHandler().useOut(picocliOut).useErr(picocliErr).asInstanceOf[IExceptionHandler2[Try[Object]]],
          args: _ *).map { _ =>
          // TODO: maybe later we want a more complete result type here, something that includes the result of parseWithHandlers as well
          picocliCommandLine.getParseResult
        }.recoverWith {
          case t: UsageHelpRequested =>
            Failure(t)
          case t: Throwable =>
            Failure(CommandLineParsingFailed(t))
        }
      } else IO()
      r <- commandLine.foldMap(executionCompiler(args, picocliCommandSpec, picocliOut, picocliErr)).run(ParamCounter()).value._2.handleErrorWith {
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
    commandSpec: PicocliCommandSpec,
    picocliOut: PrintStream,
    picocliErr: PrintStream
  ): CommandLineArgSpecA ~> CommandLineExecutionState =
    new (CommandLineArgSpecA ~> CommandLineExecutionState) {

      def apply[A](fa: CommandLineArgSpecA[A]): CommandLineExecutionState[A] =
        fa match {
          case NoSpec(result) =>
            debug(s"returning no args value $result")
            State.inspect(_ => result.asInstanceOf[IO[A]])
          case Args() =>
            State.inspect(_ => args.toList)
          case Opt(name, _, _) =>
            debug(s"getting option value $name")
            State.inspect(_ => Option(commandSpec.optionsMap.get(s"--$name").getValue[A]))
          case OptWithDefaultValue(name, _, _, _) =>
            debug(s"getting option value $name")
            State.inspect(_ => commandSpec.optionsMap.get(s"--$name").getValue[A])
          case OptWithRequiredValue(name, _, _) =>
            debug(s"getting option value $name")
            State.inspect(_ => commandSpec.optionsMap.get(s"--$name").getValue[A])
          case p @ Param(_, _, _) =>
            State.modify[ParamCounter](_.increment(p.toString)).inspect { paramCounter =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${paramCounter.counter - 1}"
                else p.toString
              Option(commandSpec.positionalParameters.find(_.descriptionKey == descriptionKey).get.getValue[A])
            }
          case p @ ParamWithDefaultValue(_, _, _, _) =>
            State.modify[ParamCounter](_.increment(p.toString)).inspect { paramCounter =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${paramCounter.counter - 1}"
                else p.toString
              commandSpec.positionalParameters.find(_.descriptionKey == descriptionKey).get.getValue[A]
            }
          case p @ ParamWithRequiredValue(_, _, _) =>
            State.modify[ParamCounter](_.increment(p.toString)).inspect { paramCounter =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${paramCounter.counter - 1}"
                else p.toString
              commandSpec.positionalParameters.find(_.descriptionKey == descriptionKey).get.getValue[A]
            }
          case p @ ParamRange(param, _, _, _, _) =>
            State.inspect(_ => commandSpec.positionalParameters.find(_.descriptionKey == System.identityHashCode(p).toString).get.getValue[A])
          case PicocliOpt(o, _) =>
            debug(s"getting option value ${o.name}")
            State.inspect(_ => Option(commandSpec.optionsMap.get(s"--${o.name}").getValue[A]))
          case PicocliOptWithDefaultValue(o, _) =>
            debug(s"getting option value ${o.name}")
            State.inspect(_ => commandSpec.optionsMap.get(s"--${o.name}").getValue[A])
          case PicocliOptWithRequiredValue(o, _) =>
            debug(s"getting option value ${o.name}")
            State.inspect(_ => commandSpec.optionsMap.get(s"--${o.name}").getValue[A])
          case CommandWithCommandLine(commandLine, _) =>
            State.inspect(_ => IO())
          case Subcommand(name, subcommandSpec, _) =>
            State.inspect(_ => Option(commandSpec.subcommands.get(name).getParseResult) match {
              case Some(parseResult) =>
                executeCommandLine(subcommandSpec, commandSpec.subcommands.get(name).getCommandSpec, args, picocliOut, picocliErr, true)
              case None =>
                debug(s"parsing args failed for subcommand $name, proceeding...")
                IO.raiseError(CommandLineParsingFailedForSubcommand(name, new RuntimeException))
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

