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
package io.jobial.sclap.example

import cats.effect.IO
import io.jobial.sclap.CommandLineApp

object TarExample extends CommandLineApp {

  def tar(create: Option[Boolean], verbose: Option[Boolean], file: Option[String]) =
    println(s"tar create $create verbose $verbose file $file")

  def run =
    command
      .header("Tar Archive")
      .description("This is a utility to manage tar archives.")
      .clusteredShortOptionsAllowed(false) {
        for {
          create <- opt[Boolean]("c")
          verbose <- opt[Boolean]("v")
          file <- opt[String]("f").alias("file")
        } yield IO {
          tar(create, verbose, file)
        }
      }

}