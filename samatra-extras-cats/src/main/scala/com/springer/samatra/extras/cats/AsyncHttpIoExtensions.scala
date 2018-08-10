package com.springer.samatra.extras.cats

import cats.effect.IO
import org.asynchttpclient.ListenableFuture

import scala.concurrent.{ExecutionContext, ExecutionException}

object AsyncHttpIoExtensions {

  implicit class IoWithFilter[A](val io: IO[A]) extends AnyVal {
    def flatMap[B](f: A => IO[B]): IO[B] = io.flatMap(f)
    def map[B](f: A => B): IO[B] = io.map(f)
    def withFilter(p: A => Boolean): IO[A] = map { r => if (p(r)) r else throw new NoSuchElementException("IO.filterWith predicate is not satisfied") }
  }

  implicit class ListenableFutureIoOps[A](val listenableFuture: ListenableFuture[A]) extends AnyVal {

    def toIO(implicit ec: ExecutionContext = ExecutionContext.global): IO[A] = IO.cancelable { cb =>

      val c = listenableFuture.addListener(
        () => cb(scala.util.Try(listenableFuture.get()).toEither.left.map {
          case exc: ExecutionException => exc.getCause
          case throwable => throwable
        }),
        (command: Runnable) => ec.execute(command)
      )

      // Cancellation logic, suspended in IO
      IO.apply({
        if (!c.isDone && !c.isCancelled) {
          c.cancel(true)
        }
      })
    }
  }
}
