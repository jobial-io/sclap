package io.jobial.sclap.core.implicits

import cats.Monoid
import io.jobial.sclap.core.ArgumentValueParser
import cats.implicits._
import scala.reflect.ClassTag

trait ArgumentValueParserInstances {
  
  implicit val stringArgumentValueParser = new ArgumentValueParserFromMonoid[String]() {
    def parse(s: String) = s
  }

  implicit val intArgumentValueParser = new ArgumentValueParserFromMonoid[Int]() {
    def parse(s: String) = s.toInt
  }

  implicit val doubleArgumentValueParser = new ArgumentValueParserFromMonoid[Double]() {
    def parse(s: String) = s.toDouble
  }

  implicit def optionArgumentValueParser[T: ArgumentValueParser] = new OptionArgumentValueParser[T]
}

class OptionArgumentValueParser[T: ArgumentValueParser] extends ArgumentValueParser[Option[T]] {
  
  val underlyingParser = implicitly[ArgumentValueParser[T]]

  def parse(s: String) = Some(underlyingParser.parse(s))

  val empty = None
}

abstract class ArgumentValueParserFromMonoid[T: ClassTag : Monoid] extends ArgumentValueParser {

  val empty = implicitly[Monoid[T]].empty
}
