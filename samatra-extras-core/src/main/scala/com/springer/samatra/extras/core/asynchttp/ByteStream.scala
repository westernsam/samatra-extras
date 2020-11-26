package com.springer.samatra.extras.core.asynchttp

import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CountDownLatch, TimeUnit, TimeoutException}
import com.springer.samatra.extras.core.asynchttp.AsyncHttpHelpers.StatusCode
import com.springer.samatra.extras.core.logging.Logger
import io.netty.handler.codec.http.HttpHeaders
import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient.AsyncHandler.State._
import org.asynchttpclient.{AsyncHandler, _}

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.CollectionHasAsScala

object ByteStream extends Logger {
  def apply(http: AsyncHttpClient, req: Request)(implicit ex: ExecutionContext): Either[Throwable, InputStream] = {
    val out = new PipedOutputStream()
    val is = new PipedInputStream(out, 8096 * 4)

    //side effect is that output stream is written to
    //is this ok? seems to work ok
    val latch = new CountDownLatch(1)
    val statusCode = new AtomicInteger(-1)
    val stream = new ByteStream(out, latch, statusCode)

    try {
      val bytesStreamRequest = http.executeRequest(req, stream)
      bytesStreamRequest
        .addListener(() => bytesStreamRequest.done() , (command: Runnable) => ex.execute(command))

      if (!latch.await(5, TimeUnit.SECONDS)) {
        stream.onCompleted()
        Left(new TimeoutException("No headers in 5 seconds"))
      } else if (statusCode.get() == 200)
        Right(is)
      else
        Left(StatusCode(statusCode.get))
    } catch {
      case t: Throwable =>
        stream.onCompleted()
        log.error(s"Error from $req", Some(t))
        Left(t)
    }
  }
}

class ByteStream(out: PipedOutputStream, latch: CountDownLatch, statusCodeSetter: AtomicInteger) extends AsyncHandler[Unit] with Logger {
  @volatile private var state = CONTINUE

  override def onCompleted(): Unit = {
    state = State.ABORT
    out.close()
  }

  override def onThrowable(t: Throwable): Unit = {
    log.error("Error getting bytes", Some(t))
    onCompleted()
  }

  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): AsyncHandler.State = {
    if (state == CONTINUE) {
      try {
        out.write(bodyPart.getBodyPartBytes)
        out.flush()
      } catch {
        case t: IOException =>
          log.error("Error writing bytes", Some(t))
          onCompleted()
      }
    }
    state
  }

  override def onStatusReceived(responseStatus: HttpResponseStatus): AsyncHandler.State = {
    val statusCode = responseStatus.getStatusCode
    statusCodeSetter.set(statusCode)

    latch.countDown()

    if (statusCode != 200) {
      onCompleted()
    }
    state
  }

  override def onHeadersReceived(headers: HttpHeaders): AsyncHandler.State = {
    headers.entries().asScala.foreach(header => log.debug(s"${header.getKey}=${header.getValue}"))
    state
  }
}