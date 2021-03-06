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
import io.jobial.sclap.core.ArgumentValueParser


object ArithmeticExample4 extends CommandLineApp {

  def operation[T: ArgumentValueParser](name: String, op: (T, T) => T) =
    subcommand[T](name) {
      for {
        a <- param[T].required
        b <- param[T].required
      } yield IO(op(a, b))
    }

  def run =
    for {
      subcommandResult <- subcommands(
        operation[Double]("add", _ + _),
        operation[Double]("sub", _ - _),
        operation[Double]("mul", _ * _),
        operation[Double]("div", _ / _)
      )
    } yield subcommandResult.map(println)

}
