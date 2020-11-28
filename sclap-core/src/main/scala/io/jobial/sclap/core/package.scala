package io.jobial.sclap

import cats.effect.IO
import cats.free.Free

package object core {
  type CommandLineArgSpec[A] = Free[CommandLineArgSpecA, A]
  
  type CommandLine[A] = CommandLineArgSpec[IO[A]]
}
