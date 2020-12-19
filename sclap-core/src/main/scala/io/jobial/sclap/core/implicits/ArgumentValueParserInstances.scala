package io.jobial.sclap.core.implicits

import cats.Monoid
import io.jobial.sclap.core.ArgumentValueParser
import cats.implicits._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag

trait ArgumentValueParserInstances {

  implicit val stringArgumentValueParser =
    new ArgumentValueParserFromMonoid[String]() {
      def parse(s: String) = s
    }

  implicit val intArgumentValueParser =
    new ArgumentValueParserFromMonoid[Int]() {
      def parse(s: String) = s.toInt
    }

  implicit val longArgumentValueParser =
    new ArgumentValueParserFromMonoid[Long]() {
      def parse(s: String) = s.toLong
    }

  implicit val floatArgumentValueParser =
    new ArgumentValueParserFromMonoid[Float]() {
      def parse(s: String) = s.toFloat
    }

  implicit val doubleArgumentValueParser =
    new ArgumentValueParserFromMonoid[Double]() {
      def parse(s: String) = s.toDouble
    }

  implicit val durationArgumentValueParser =
    new ArgumentValueParserFromMonoid[Duration]() {
      def parse(s: String) = Duration(s)
    }

  implicit val finiteDurationArgumentValueParser =
    new ArgumentValueParserFromMonoid[FiniteDuration]() {
      def parse(s: String) = Duration(s) match {
        case d: FiniteDuration =>
          d
      }
    }

  implicit val booleanArgumentValueParser =
    new ArgumentValueParser[Boolean]() {
      def parse(s: String) = s.toBoolean

      override def empty = false
    }

  implicit def optionArgumentValueParser[T: ArgumentValueParser] = new OptionArgumentValueParser[T]

}

class OptionArgumentValueParser[T: ArgumentValueParser] extends ArgumentValueParser[Option[T]] {

  def parse(s: String) = Some(ArgumentValueParser[T].parse(s))

  val empty = None
}

abstract class ArgumentValueParserFromMonoid[T: ClassTag : Monoid] extends ArgumentValueParser[T] {

  val empty = Monoid[T].empty
}
