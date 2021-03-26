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


object ArithmeticExample2 extends CommandLineApp {

  def operation(name: String, op: (Int, Int) => Int) =
    subcommand[Int](name) {
      for {
        a <- param[Int].required
        b <- param[Int].required
      } yield IO(op(a, b))
    }

  def run =
    for {
      addResult <- operation("add", _ + _)
      subResult <- operation("sub", _ - _)
      mulResult <- operation("mul", _ * _)
      divResult <- operation("div", _ / _)
    } yield for {
      r <- addResult orElse subResult orElse mulResult orElse divResult
    } yield println(r)

}
