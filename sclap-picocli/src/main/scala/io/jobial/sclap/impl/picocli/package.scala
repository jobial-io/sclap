package io.jobial.sclap.impl

import cats.~>
import io.jobial.sclap.core.{CommandLineArgSpec, CommandLineArgSpecA}

package object picocli {
  
  def mapSingleCommandLineArgSpec[T, X <: CommandLineArgSpecA[T]](argSpec: CommandLineArgSpec[T])(f: X => CommandLineArgSpecA[T]) =
    argSpec.mapK(new (CommandLineArgSpecA ~> CommandLineArgSpecA) {
      def apply[A](fa: CommandLineArgSpecA[A]): CommandLineArgSpecA[A] =
        f(fa.asInstanceOf[X])
          .asInstanceOf[CommandLineArgSpecA[A]]
    })
}
