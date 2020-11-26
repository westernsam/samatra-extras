package com.springer.samatra.extras.core.asynchttp

import java.nio.charset.StandardCharsets

import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient._

import scala.concurrent.{ExecutionContext, ExecutionException, Future}

object AsyncHttpHelpers {
  case class StatusCode(code: Int)
    extends Exception("Unexpected response status: %d".format(code))

  class FunctionHandler[T](f: Response => T) extends AsyncCompletionHandler[T] {
    def onCompleted(response: Response): T = f(response)
  }

  trait OkHandler[T] extends AsyncHandler[T] {
    abstract override def onStatusReceived(status: HttpResponseStatus): State = {
      if (status.getStatusCode / 100 == 2)
        super.onStatusReceived(status)
      else
        throw StatusCode(status.getStatusCode)
    }
  }

  class OkFunctionHandler[T](f: Response => T)
    extends FunctionHandler[T](f) with OkHandler[T]

  // AsyncHandler implementations are not necessarily thread safe. E.g. AsyncCompletionHandler has mutable state with a
  // non-shareable response builder
  def ensureOk = new OkFunctionHandler(identity)
  def ensureOkBodyAsString = new OkFunctionHandler(_.getResponseBody)
  def ensureOkBodyAsUtf8String = new OkFunctionHandler(_.getResponseBody(StandardCharsets.UTF_8))

  implicit class ListenableFutureOps[A](val listenableFuture: ListenableFuture[A]) extends AnyVal {
    def toFuture(implicit executor: ExecutionContext): Future[A] = {
      val promise = scala.concurrent.Promise[A]()
      listenableFuture.addListener(
        new Runnable {
          override def run(): Unit = {
            promise.complete(scala.util.Try(listenableFuture.get()))
          }
        },
        new java.util.concurrent.Executor {
          def execute(runnable: Runnable) :Unit = {
            executor.execute(runnable)
          }
        }
      )
      promise.future
    }

    def toFutureEither(implicit executor: ExecutionContext): Future[Either[Throwable, A]] = toFuture.map { res => Right(res) }.recover {
      case exc: ExecutionException => Left(exc.getCause)
      case throwable => Left(throwable)
    }
  }
}
