package com.springer.samatra.extras.cats

import cats.effect.{Async, IO}
import org.asynchttpclient.ListenableFuture

import scala.concurrent.{ExecutionContext, ExecutionException}

object AsyncHttpCatsAsycnExtensions {

  implicit class IoWithFilter[A](val io: IO[A]) extends AnyVal {
    def flatMap[B](f: A => IO[B]): IO[B] = io.flatMap(f)
    def map[B](f: A => B): IO[B] = io.map(f)
    def withFilter(p: A => Boolean): IO[A] = map { r => if (p(r)) r else throw new NoSuchElementException("IO.filterWith predicate is not satisfied") }
  }

  implicit class ListenableFutureIoOps[A](val listenableFuture: ListenableFuture[A]) extends AnyVal {

    def toAsync[F[_]: Async](implicit ex: ExecutionContext = scala.concurrent.ExecutionContext.global): F[A] = Async[F].async { cb =>

      val c = listenableFuture.addListener(
        () => cb(scala.util.Try(listenableFuture.get()).toEither.left.map {
          case exc: ExecutionException => exc.getCause
          case throwable => throwable
        }),
        (command: Runnable) => ex.execute(command)
      )

      // Cancellation logic, suspended in IO
      Async[F].delay(Some(Async[F].delay({
        if (!c.isDone && !c.isCancelled) {
          c.cancel(true)
        }
      })))
    }
  }
}
