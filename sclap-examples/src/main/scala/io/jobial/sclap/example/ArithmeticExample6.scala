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
import io.jobial.sclap.core.ArgumentValueParser


object ArithmeticExample6 extends CommandLineApp {

  def operation[T: ArgumentValueParser](name: String, op: (T, T) => T) =
    subcommand[T](name)
      .header(s"${name.toUpperCase} two numbers.")
      .description("Speficy the two operands and the result will be printed.") {
        for {
          a <- param[T].description("The first operand.").required
          b <- param[T].description("The second operand.").required
        } yield op(a, b)
      }

  def run =
    command("arithmetic")
      .header("Simple arithmetics on the command line.")
      .description("Use the following commands to add, subtract, multiply, divide numbers.") {
        for {
          subcommandResult <- subcommands(
            operation[Double]("add", _ + _),
            operation[Double]("sub", _ - _),
            operation[Double]("mul", _ * _),
            operation[Double]("div", _ / _)
          )
        } yield subcommandResult.map(println)
      }

}
