package io.jobial.sclap.core.implicits

import scala.util.{Failure, Success, Try}

trait TryExtensionInstance {

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
}
