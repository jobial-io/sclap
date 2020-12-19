package io.jobial.sclap.example

import io.jobial.sclap.CommandLineApp

import scala.concurrent.duration._

object PingExample extends CommandLineApp {

  def myPing(host: String, count: Int, timeout: FiniteDuration, timeToLive: Option[Int]) =
    println(s"pinging $host with $count packets, $timeout timeout and $timeToLive ttl...")

  def run =
    for {
      count <- opt("-count", 10)
      timeout <- opt("-timeout", 5 seconds)
      timeToLive <- opt[Int]("-ttl")
      host <- param[String].required
    } yield
      myPing(host, count, timeout, timeToLive)

}