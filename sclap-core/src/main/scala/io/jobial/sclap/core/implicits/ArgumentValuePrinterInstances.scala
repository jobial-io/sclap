package io.jobial.sclap.core.implicits

import cats.Show
import io.jobial.sclap.core.ArgumentValuePrinter

import scala.concurrent.duration.{Duration, FiniteDuration}

trait ArgumentValuePrinterInstances {

  implicit def argumentValuePrinterFromShow[T: Show] =
    new ArgumentValuePrinterFromShow[T] {}

  implicit def durationArgumentValuePrinter =
    new ArgumentValuePrinter[Duration] {
      def print(value: Duration) = value.toString
    }

  implicit def finiteDurationArgumentValuePrinter =
    new ArgumentValuePrinter[FiniteDuration] {
      def print(value: FiniteDuration) = value.toString
    }
}

class ArgumentValuePrinterFromShow[T: Show] extends ArgumentValuePrinter[T] {

  def print(value: T) = Show[T].show(value)
}

