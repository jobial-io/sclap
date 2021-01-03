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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object FutureExample extends CommandLineApp {

  def run =
    for {
      hello <- opt[String]("--hello")
    } yield Future {
      println(s"hello $hello")
    }
    
}
