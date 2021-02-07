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

import scala.concurrent.duration._

object PingExample extends CommandLineApp {

  def run =
    for {
      count <- opt("count", 10).description("Number of packets")
      timeout <- opt("timeout", 5.seconds).description("The timeout")
      timeToLive <- opt[Int]("ttl").description("Time to live")
      host <- param[String].paramLabel("<hostname>")
        .description("The host").required
    } yield
      myPing(host, count, timeout, timeToLive)

  def myPing(host: String, count: Int, timeout: FiniteDuration, timeToLive: Option[Int]) =
    println(s"Pinging $host with $count packets, $timeout timeout and $timeToLive ttl...")

}