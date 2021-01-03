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
