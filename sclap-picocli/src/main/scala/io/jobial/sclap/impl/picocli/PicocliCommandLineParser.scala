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
import scala.reflect.ClassTag
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}


trait PicocliCommandLineParser {
  this: CommandLineParserSyntax with Logging =>

  def parse[A](commandLine: CommandLine[A], args: Seq[String]) = {
    val result = commandLine.foldMap(parserCompiler(args)).run(PicocliCommandSpec.create.mixinStandardHelpOptions(true))

    result.value._1
  }

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
    executeCommandLine(commandLine, parse(commandLine, args), args, picocliOut, picocliErr)

  val sclapLoggingEnabled = false

  private def debug(message: => String) =
    if (sclapLoggingEnabled)
      logger.debug(message)

  def executeCommandLine[A](
    commandLine: CommandLine[A],
    picocliCommandSpec: PicocliCommandSpec,
    args: Seq[String],
    picocliOut: PrintStream,
    picocliErr: PrintStream,
    subcommand: Boolean = false
  ) =
    for {
      picocliCommandLine <- IO(new PicocliCommandLine(picocliCommandSpec)
        .registerConverter(classOf[BigDecimal], new PicocliCommandLine.ITypeConverter[BigDecimal] {
          override def convert(s: String) = BigDecimal(s)
        })
        .registerConverter(classOf[LocalDate], new PicocliCommandLine.ITypeConverter[LocalDate] {
          override def convert(s: String) = LocalDate.parse(s)
        })
        .registerConverter(classOf[LocalTime], new PicocliCommandLine.ITypeConverter[LocalTime] {
          override def convert(s: String) = new LocalTime(s)
        })
        .registerConverter(classOf[LocalDateTime], new PicocliCommandLine.ITypeConverter[LocalDateTime] {
          override def convert(s: String) = LocalDateTime.parse(s)
        })
        .registerConverter(classOf[Duration], new PicocliCommandLine.ITypeConverter[Duration] {
          override def convert(s: String) = new Duration(s)
        })
        .registerConverter(classOf[Period], new PicocliCommandLine.ITypeConverter[Period] {
          override def convert(s: String) = new Period(s)
        })
        .registerConverter(classOf[Regex], new PicocliCommandLine.ITypeConverter[Regex] {
          override def convert(s: String) = new Regex(s)
        }))
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

  type CommandLineProcessingState[A] = State[PicocliCommandSpec, A]

  def parserCompiler(args: Seq[String]): CommandLineArgSpecA ~> CommandLineProcessingState = new (CommandLineArgSpecA ~> CommandLineProcessingState) {

    def apply[A](fa: CommandLineArgSpecA[A]): CommandLineProcessingState[A] = {

      def optionSpecBuilder(names: String*) = OptionSpec.builder(names.toArray)

      def optionSpecBuilderForOpt[T](opt: OptSpec[_, T], resultClass: Class[_]) = {
        val specBuilder = optionSpecBuilder(s"--${opt.name}")
          // TODO: get rid of this by directly registering option type converter
          .`type`(resultClass)
        opt.paramLabel.map(l => specBuilder.paramLabel(l)).getOrElse(specBuilder)
      }

      def addOptionSpec[T](specBuilder: OptionSpec.Builder, empty: T) = {
        val spec = specBuilder.build
        debug(s"adding $spec")
        State.modify[PicocliCommandSpec](_.addOption(spec)).inspect(_ => empty)
      }

      def addOpt[T: ArgumentValueParser](opt: Opt[T], builder: OptionSpec.Builder => OptionSpec.Builder = identity[OptionSpec.Builder]) =
        addOptionSpec(builder(optionSpecBuilderForOpt(opt, opt.parser.resultClass)), Option(ArgumentValueParser[T].empty))

      def addOptWithDefaultValue[T: ArgumentValueParser](opt: OptWithDefaultValue[T], builder: OptionSpec.Builder => OptionSpec.Builder = identity[OptionSpec.Builder]) =
        addOptionSpec(builder(optionSpecBuilderForOpt(opt, opt.parser.resultClass).defaultValue(opt.defaultValuePrinter.print(opt.defaultValue))), ArgumentValueParser[T].empty)

      def addOptWithRequiredValue[T: ArgumentValueParser](opt: OptWithRequiredValue[T], builder: OptionSpec.Builder => OptionSpec.Builder = identity[OptionSpec.Builder]) =
        addOptionSpec(builder(optionSpecBuilderForOpt(opt, opt.parser.resultClass).required(true)), ArgumentValueParser[T].empty)

      def addParamSpec[T](specBuilder: PositionalParamSpec.Builder, empty: T) = {
        State.modify[PicocliCommandSpec] { picocliCommandSpec =>
          val paramNum = picocliCommandSpec.positionalParameters().size
          if (specBuilder.index.isUnspecified) {
            specBuilder.descriptionKey(s"${specBuilder.descriptionKey}_${paramNum}")
            println("added description key " + specBuilder.descriptionKey())
            specBuilder.index(paramNum.toString)
          }
          val spec = specBuilder.build
          debug(s"adding $spec")
          picocliCommandSpec.addPositional(spec)
        }.inspect(_ => empty)
      }

      def paramSpecBuilder[T](param: ParamSpec[_, T], resultClass: Class[_]) = {
        val specBuilder = PositionalParamSpec.builder
          // TODO: get rid of this by directly registering option type converter
          .`type`(resultClass).descriptionKey(param.toString).required(false)
        param.paramLabel.map(l => specBuilder.paramLabel(l)).getOrElse(specBuilder)
        param.description.map(l => specBuilder.description(l)).getOrElse(specBuilder)
      }

      def addParam[T](param: Param[T], builder: PositionalParamSpec.Builder => PositionalParamSpec.Builder = identity[PositionalParamSpec.Builder]) =
        addParamSpec(builder(paramSpecBuilder(param, param.parser.resultClass).required(false).index(param.index.map(_.toString).getOrElse(""))), Option(param.parser.empty))

      def addParamWithDefaultValue[T: ArgumentValuePrinter](param: ParamWithDefaultValue[T], builder: PositionalParamSpec.Builder => PositionalParamSpec.Builder = identity[PositionalParamSpec.Builder]) =
        addParamSpec(builder(paramSpecBuilder(param, param.parser.resultClass).required(false).index(param.index.map(_.toString).getOrElse("")).defaultValue(ArgumentValuePrinter[T].print(param.defaultValue))), param.parser.empty)

      def addParamWithRequiredValue[T](param: ParamWithRequiredValue[T], builder: PositionalParamSpec.Builder => PositionalParamSpec.Builder = identity[PositionalParamSpec.Builder]) =
        addParamSpec(builder(paramSpecBuilder(param, param.parser.resultClass).required(true).index(param.index.map(_.toString).getOrElse(""))), param.parser.empty)

      fa match {
        case opt: Opt[A] =>
          addOpt(opt)(opt.parser)
        case opt: OptWithDefaultValue[A] =>
          addOptWithDefaultValue(opt)(opt.parser)
        case opt: OptWithRequiredValue[A] =>
          addOptWithRequiredValue(opt)(opt.parser)
        case param: Param[A] =>
          addParam(param)
        case param: ParamWithDefaultValue[A] =>
          addParamWithDefaultValue(param)(param.defaultValuePrinter)
        case param: ParamWithRequiredValue[A] =>
          addParamWithRequiredValue(param, _.required(true))
        case PicocliOpt(opt, builder) =>
          addOpt(opt, builder)(opt.parser)
        case PicocliOptWithDefaultValue(opt, builder) =>
          addOptWithDefaultValue(opt, builder)(opt.parser)
        case PicocliOptWithRequiredValue(opt, builder) =>
          addOptWithRequiredValue(opt, builder)(opt.parser)
        case CommandWithCommandLine(_, command) =>
          State.modify[PicocliCommandSpec] { spec =>
            command.header.map(spec.usageMessage().header(_))
            spec
          }.modify[PicocliCommandSpec] { spec =>
            command.description.map(spec.usageMessage().description(_))
            spec
          }.inspect(_ => IO())
        case Subcommand(name, subcommandLine, aliases) =>
          val r = parse(subcommandLine, args)
          val picocliSubcommandLine = new PicocliCommandLine(r)
          aliases.map(aliases => picocliSubcommandLine.getCommandSpec.aliases(aliases: _*))
          State.modify[PicocliCommandSpec](_.addSubcommand(name, picocliSubcommandLine)).inspect(_ => IO())
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
              println(descriptionKey)
              Option(commandSpec.positionalParameters.find(_.descriptionKey == descriptionKey).get.getValue[A])
            }
          case p @ ParamWithDefaultValue(_, _, _, _) =>
            State.modify[ParamCounter](_.increment(p.toString)).inspect { paramCounter =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${paramCounter.counter - 1}"
                else p.toString
              println(descriptionKey)
              commandSpec.positionalParameters.find(_.descriptionKey == descriptionKey).get.getValue[A]
            }
          case p @ ParamWithRequiredValue(_, _, _) =>
            State.modify[ParamCounter](_.increment(p.toString)).inspect { paramCounter =>
              val descriptionKey =
                if (p.index.isEmpty)
                  s"${p.toString}_${paramCounter.counter - 1}"
                else p.toString
              println(descriptionKey)
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

  // do we need this?
  abstract class ArgumentValueParserFromConverter[T: ClassTag](converter: ITypeConverter[T]) extends ArgumentValueParser {

    def parse(s: String) = converter.convert(s)
  }

}

case class PicocliOpt[T: ArgumentValueParser](opt: Opt[T], builder: OptionSpec.Builder => OptionSpec.Builder)
  extends CommandLineArgSpecWithArgA[Option[T], T]

case class PicocliOptWithDefaultValue[T: ArgumentValueParser](opt: OptWithDefaultValue[T], builder: OptionSpec.Builder => OptionSpec.Builder)
  extends CommandLineArgSpecWithArgA[T, T]()

case class PicocliOptWithRequiredValue[T: ArgumentValueParser](opt: OptWithRequiredValue[T], builder: OptionSpec.Builder => OptionSpec.Builder)
  extends CommandLineArgSpecWithArgA[T, T]()

