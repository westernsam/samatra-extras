package com.springer.samatra.extras.cats

import java.util.concurrent.atomic.AtomicReference

import cats.effect.IO
import cats.effect.IO.shift
import com.springer.samatra.routing.FutureResponses.{Rendering, Running, State, TimingOutListener}
import com.springer.samatra.routing.Routings.HttpResp
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{AsyncContext, AsyncEvent}

import scala.concurrent.ExecutionContext

object IoResponses {

  case class Timeout(t: Long)
  case class ResponseOnTimeout(statusCode: Int)

  implicit class IoResponse[A](io: IO[A])(implicit rest: A => HttpResp, ex: ExecutionContext = ExecutionContext.global, timeout: Timeout = Timeout(5000), responseOnTimeout: ResponseOnTimeout = ResponseOnTimeout(500)) extends HttpResp {
    override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      val state = new AtomicReference[State](Running)

      val async: AsyncContext = req.startAsync(req, resp)
      async.setTimeout(timeout.t)

      val cancel: () => Unit = shift(ex).flatMap(_ => io).unsafeRunCancelable(_.fold(
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
      ))

      async.addListener(new TimingOutListener(state, timeout.t, false, responseOnTimeout.statusCode) {
        override def onTimeout(event: AsyncEvent): Unit = {
          super.onTimeout(event)
          cancel()
        }
      })
    }
  }

}
