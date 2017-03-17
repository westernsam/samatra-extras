package com.springer.samatra.extras.metrics

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{AsyncEvent, AsyncListener}

import com.springer.samatra.extras.Logger
import com.timgroup.statsd.StatsDClient
import org.eclipse.jetty.server.handler.{AbstractHandler, HandlerWrapper}
import org.eclipse.jetty.server.{AsyncContextEvent, HttpChannelState, Request}

object MetricsHandler {
  def isInternal(request: HttpServletRequest): Boolean = request.getRequestURI.startsWith("/internal")
}

class MetricsHandler(statsdClient: MetricsStatsdClient, handler: AbstractHandler, ignore: HttpServletRequest => Boolean) extends HandlerWrapper {

  setHandler(handler)

  val asyncRecorder = new AsyncListener {
    override def onError(event: AsyncEvent): Unit = ()
    override def onComplete(event: AsyncEvent): Unit = record(event.asInstanceOf[AsyncContextEvent].getHttpChannelState.getBaseRequest)
    override def onStartAsync(event: AsyncEvent): Unit = ()
    override def onTimeout(event: AsyncEvent): Unit = ()
  }

  def record(req: Request) {
    statsdClient.incrementCounter(s"webapp.responses.${responseCode(req)}xx")
    val duration = System.currentTimeMillis - req.getTimeStamp
    statsdClient.recordExecutionTime("webapp.responsetime", duration)
  }

  override def handle(target: String, request: Request, httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Unit = {
    if (!ignore(request)) statsdClient.incrementCounter("webapp.requests")

    val state: HttpChannelState = request.getHttpChannelState

    try {
      super.handle(target, request, httpRequest, httpResponse)
    } finally if (!ignore(request)) {
      if (state.isSuspended) {
        if (state.isInitial) {
          state.addListener(asyncRecorder)
        }
      } else if (state.isInitial) record(request)
    }
  }

  private def responseCode(request: Request): Int = request.getResponse.getStatus / 100
}

trait MetricsStatsdClient {
  def incrementCounter(name: String)

  def gauge(name: String, value: Long)

  def recordExecutionTime(name: String, duration: Long)
}

object MetricsStatsdClient extends Logger {
  def newStatsDClient(statsd: StatsDClient, env: String) = new MetricsStatsdClient {
    override def incrementCounter(name: String): Unit =
      if (!env.equals("LOCAL")) statsd.incrementCounter(name)
      else log.info(s"Incrementing metrics counter $name")

    override def gauge(name: String, value: Long): Unit =
      if (!env.equals("LOCAL")) statsd.gauge(name, value)
      else log.info(s"Recording gauge metric ($name, $value)")

    override def recordExecutionTime(name: String, duration: Long): Unit =
      if (!env.equals("LOCAL")) statsd.recordExecutionTime(name, duration)
      else log.info(s"Recording execution time metric ($name, $duration)")
  }
}