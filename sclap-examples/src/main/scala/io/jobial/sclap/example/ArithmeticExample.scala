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

import io.jobial.sclap.CommandLineApp
import cats.effect.IO

object ArithmeticExample extends CommandLineApp {

  def add =
    command.header("Add two numbers") {
      for {
        a <- opt[Int]("a").required
        b <- opt[Int]("b").required
      } yield IO(a + b)
    }

  def sub =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(a - b)

  def mul =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(a * b)

  def div =
    for {
      a <- opt[Int]("a").required
      b <- opt[Int]("b").required
    } yield IO(a / b)

  def run =
    for {
      addResult <- subcommand("add")(add)
      subResult <- subcommand("sub")(sub)
      mulResult <- subcommand("mul")(mul)
      divResult <- subcommand("div")(div)
    } yield for {
      r <- addResult orElse subResult orElse mulResult orElse divResult
    } yield println(r)
}
