package io.jobial.sclap

import cats.effect.{ExitCode, IO, IOApp}
import io.jobial.sclap.core.{CommandLine, HelpRequested, UsageHelpRequested, VersionHelpRequested}

trait CommandLineAppNoImplicits
  extends IOApp
    with CommandLineParserNoImplicits {

  def run: CommandLine[Any]

  def run(args: List[String]): IO[ExitCode] =
    for {
      result <- executeCommandLine(run, args) handleErrorWith {
        case t: HelpRequested =>
          IO(ExitCode.Success)
        case t =>
          IO(ExitCode.Error)
      }
    } yield result match {
      case code: ExitCode =>
        code
      case _ =>
        ExitCode.Success
    }
}
