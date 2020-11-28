package io.jobial.sclap

import cats.effect._
import org.scalatest.{Assertion, Succeeded}

import scala.concurrent.Future

object EffectTest extends App {

  import cats.effect.{Effect, SyncIO, IO}

  val task = IO("Hello World!")

  val ioa: SyncIO[Unit] =
    Effect[IO].runAsync(task) {
      case Right(value) => IO(println(value))
      case Left(error)  => IO.raiseError(error)
    }

  
  task.unsafeRunSync()  
  Thread.sleep(10000)
  ioa.unsafeRunSync()
//
//  implicit def syncIoToFutureAssertion(io: SyncIO[Assertion]): Future[Assertion] =
//    io.toIO.unsafeToFuture()
//  implicit def ioToFutureAssertion(io: IO[Assertion]): Future[Assertion] =
//    io.unsafeToFuture()
//  implicit def syncIoUnitToFutureAssertion(io: SyncIO[Unit]): Future[Assertion] =
//    io.toIO.as(Succeeded).unsafeToFuture()
//  implicit def ioUnitToFutureAssertion(io: IO[Unit]): Future[Assertion] =
//    io.as(Succeeded).unsafeToFuture()
}
