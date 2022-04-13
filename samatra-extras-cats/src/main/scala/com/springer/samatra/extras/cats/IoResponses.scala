package com.springer.samatra.extras.cats

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.springer.samatra.routing.FutureResponses.{Rendering, Running, State, TimingOutListener}
import com.springer.samatra.routing.Routings.HttpResp

import java.util.concurrent.atomic.AtomicReference
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{AsyncContext, AsyncEvent}

object IoResponses {

  case class Timeout(t: Long)

  case class ResponseOnTimeout(statusCode: Int)

  def fromIOWithTimeout[T](fa: IO[T])(timeout: Timeout, responseOnTimeout: ResponseOnTimeout = ResponseOnTimeout(500))(implicit rest: T => HttpResp, rt: IORuntime): HttpResp =
    IoResponse(fa)(rest, timeout, responseOnTimeout, rt)

  implicit class IoResponse[A](io: IO[A])(implicit rest: A => HttpResp, timeout: Timeout = Timeout(5000), responseOnTimeout: ResponseOnTimeout = ResponseOnTimeout(500), rt: IORuntime) extends HttpResp {
    override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      if (req.isAsyncStarted) {
        throw new IllegalStateException("Async already started. Have you wrapper a IOResponse inside another IOResponse?")
      } else {

        val state = new AtomicReference[State](Running)

        val async: AsyncContext = req.startAsync(req, resp)
        async.setTimeout(timeout.t)

        val cancel =
          io.attempt.map(_.fold(
            err => {
              if (state.getAndSet(Rendering) == Running) {
                val asyncResponse: HttpServletResponse = async.getResponse.asInstanceOf[HttpServletResponse]
                req.setAttribute("javax.servlet.error.exception", err)
                try {
                  asyncResponse.sendError(500)
                } finally {
                  async.complete()
                }
              }
            },
            a => {
              if (state.getAndSet(Rendering) == Running) {
                val asyncResponse: HttpServletResponse = async.getResponse.asInstanceOf[HttpServletResponse]
                val asyncRequest: HttpServletRequest = async.getRequest.asInstanceOf[HttpServletRequest]

                try {
                  rest(a).process(asyncRequest, asyncResponse)
                } finally {
                  async.complete()
                }
              }
            }
          )).unsafeRunCancelable()

        async.addListener(new TimingOutListener(state, timeout.t, false, responseOnTimeout.statusCode) {
          override def onTimeout(event: AsyncEvent): Unit = {
            super.onTimeout(event)
            cancel()
          }
        })
      }
    }
  }

}
