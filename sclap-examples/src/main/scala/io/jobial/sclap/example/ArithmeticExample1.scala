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
import io.jobial.sclap.core.CommandLine


object ArithmeticExample1 extends CommandLineApp {

  def operation(op: (Int, Int) => Int): CommandLine[Int] =
    for {
      a <- opt[Int]("-a").required
      b <- opt[Int]("-b").required
    } yield IO(op(a, b))

  def run =
    for {
      addResult <- subcommand("add")(operation(_ + _))
      subResult <- subcommand("sub")(operation(_ - _))
      mulResult <- subcommand("mul")(operation(_ * _))
      divResult <- subcommand("div")(operation(_ / _))
    } yield for {
      r <- addResult orElse subResult orElse mulResult orElse divResult
    } yield println(r)

}
