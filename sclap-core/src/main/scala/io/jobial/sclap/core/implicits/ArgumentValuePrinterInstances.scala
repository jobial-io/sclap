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
package io.jobial.sclap.core.implicits

import cats.Show
import io.jobial.sclap.core.ArgumentValuePrinter

import java.io.File
import scala.reflect.ClassTag

trait ArgumentValuePrinterInstances {

  implicit def argumentValuePrinterFromShow[T: Show] =
    new ArgumentValuePrinterFromShow[T] {}
     
  implicit val fileArgumentValuePrinter =
    new ArgumentValuePrinter[File] {
      def print(value: File) =
        value.getPath
    }

  implicit def optionArgumentValuePrinter[T: ArgumentValuePrinter] =
    new ArgumentValuePrinter[Option[T]] {
      def print(value: Option[T]) = value.map(ArgumentValuePrinter[T].print).getOrElse("")
    }

  implicit def enumArgumentValuePrinter[T <: Enum[T] : ClassTag] = new ArgumentValuePrinter[T] {
    def print(value: T) = value.toString
  }
}

class ArgumentValuePrinterFromShow[T: Show] extends ArgumentValuePrinter[T] {

  def print(value: T) = Show[T].show(value)
}

class ListArgumentValuePrinter[T: ArgumentValuePrinter](separator: String) extends ArgumentValuePrinter[List[T]] {

  def print(value: List[T]) =
    value.map(ArgumentValuePrinter[T].print).mkString(separator)
}
