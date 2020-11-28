package io.jobial.sclap.core.implicits

import cats.Show
import io.jobial.sclap.core.ArgumentValuePrinter

trait ArgumentValuePrinterInstances {

  implicit def argumentValuePrinterFromShow[T: Show] =
    new ArgumentValuePrinterFromShow[T] {}
}

class ArgumentValuePrinterFromShow[T: Show] extends ArgumentValuePrinter[T] {

  def print(value: T) = Show[T].show(value)
}

