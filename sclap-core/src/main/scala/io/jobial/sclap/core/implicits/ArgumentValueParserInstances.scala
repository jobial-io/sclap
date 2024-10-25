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

import cats.Monoid
import cats.implicits._
import io.jobial.sclap.core.ArgumentValueParser

import java.io.File
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.util.Try

trait ArgumentValueParserInstances extends TryExtensionInstance {

  implicit val stringArgumentValueParser: ArgumentValueParserFromMonoid[String] =
    new ArgumentValueParserFromMonoid[String] {
      def parse(s: String) = Right(s)
    }

  implicit val intArgumentValueParser: ArgumentValueParserFromMonoid[Int] =
    new ArgumentValueParserFromMonoid[Int] {
      def parse(s: String) = Try(s.toInt).toEither
    }

  implicit val longArgumentValueParser: ArgumentValueParserFromMonoid[Long] =
    new ArgumentValueParserFromMonoid[Long] {
      def parse(s: String) = Try(s.toLong).toEither
    }

  implicit val floatArgumentValueParser: ArgumentValueParserFromMonoid[Float] =
    new ArgumentValueParserFromMonoid[Float] {
      def parse(s: String) = Try(s.toFloat).toEither
    }

  implicit val doubleArgumentValueParser: ArgumentValueParserFromMonoid[Double] =
    new ArgumentValueParserFromMonoid[Double] {
      def parse(s: String) = Try(s.toDouble).toEither
    }

  implicit val bigDecimalArgumentValueParser: ArgumentValueParserFromMonoid[BigDecimal] =
    new ArgumentValueParserFromMonoid[BigDecimal] {
      def parse(s: String) = Try(BigDecimal(s)).toEither
    }

  implicit val durationArgumentValueParser: ArgumentValueParserFromMonoid[Duration] =
    new ArgumentValueParserFromMonoid[Duration] {
      def parse(s: String) = Try(Duration(s)).toEither
    }

  implicit val finiteDurationArgumentValueParser: ArgumentValueParserFromMonoid[FiniteDuration] =
    new ArgumentValueParserFromMonoid[FiniteDuration] {
      def parse(s: String) = Try(Duration(s) match {
        case d: FiniteDuration =>
          d
      }).toEither
    }

  implicit val booleanArgumentValueParser: ArgumentValueParser[Boolean] =
    new ArgumentValueParser[Boolean] {
      def parse(s: String) = Try(s.toBoolean).toEither

      override def empty = false
    }

  implicit def optionArgumentValueParser[T: ArgumentValueParser] = new OptionArgumentValueParser[T]

  implicit val fileArgumentValueParser: ArgumentValueParser[File] =
    new ArgumentValueParser[File] {

      def parse(value: String) =
        Try(new File(value)).toEither

      def empty = new File(".")
    }

  implicit def enumArgumentValueParser[T <: Enum[T] : ClassTag] = new ArgumentValueParser[T] {

    def parse(value: String) =
      resultClass.getEnumConstants.find(_.name === value).toRight(new IllegalArgumentException(s"Invalid value: $value"))

    def empty =
      resultClass.getEnumConstants.headOption.getOrElse(throw new IllegalArgumentException(s"Empty enum $resultClass is not supported"))
  }
}

class OptionArgumentValueParser[T: ArgumentValueParser] extends ArgumentValueParser[Option[T]] {

  def parse(s: String) =
    if (s.isEmpty) Right(None) else ArgumentValueParser[T].parse(s).map(Option(_))

  val empty = None
}

abstract class ArgumentValueParserFromMonoid[T: ClassTag : Monoid] extends ArgumentValueParser[T] {

  val empty = Monoid[T].empty
}

class ListArgumentValueParser[T: ClassTag : ArgumentValueParser](separator: String, limit: Int = -1) extends ArgumentValueParser[List[T]] {

  def parse(value: String) =
    value.split(separator, limit).map(ArgumentValueParser[T].parse).toList.sequence

  def empty = Nil
}