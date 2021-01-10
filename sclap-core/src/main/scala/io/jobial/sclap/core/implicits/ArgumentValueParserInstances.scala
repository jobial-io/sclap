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
import io.jobial.sclap.core.ArgumentValueParser
import cats.implicits._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait ArgumentValueParserInstances {

  /**
   * This is only needed for Scala 2.11 compatibility.
   *
   * @param t
   * @tparam T
   */
  implicit class TryExtension[T](t: Try[T]) {

    def toEither = t match {
      case Success(value) =>
        Right(value)
      case Failure(t) =>
        Left(t)
    }
  }

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

}

class OptionArgumentValueParser[T: ArgumentValueParser] extends ArgumentValueParser[Option[T]] {

  def parse(s: String) = ArgumentValueParser[T].parse(s).map(Some(_))

  val empty = None
}

abstract class ArgumentValueParserFromMonoid[T: ClassTag : Monoid] extends ArgumentValueParser[T] {

  val empty = Monoid[T].empty
}
