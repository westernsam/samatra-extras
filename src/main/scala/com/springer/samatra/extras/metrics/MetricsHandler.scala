package com.springer.samatra.extras.metrics

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{AsyncEvent, AsyncListener, ServletResponse}

import com.springer.samatra.extras.metrics.MetricsHandler.isInternal
import com.springer.samatra.routing.Routings.{PathParamsRoute, RegexRoute, Routes}
import org.eclipse.jetty.server.handler.{AbstractHandler, HandlerWrapper}
import org.eclipse.jetty.server.{AsyncContextEvent, HttpChannelState, Request}

object MetricsHandler {
  def isInternal(request: HttpServletRequest): Boolean = request.getRequestURI.startsWith("/internal")
}

abstract class BaseMetricsHandler(statsdClient: MetricsStatsdClient, handler: AbstractHandler, ignore: HttpServletRequest => Boolean) extends HandlerWrapper {

  setHandler(handler)

  val asyncRecorder = new AsyncListener {
    override def onError(event: AsyncEvent): Unit = ()
    override def onComplete(event: AsyncEvent): Unit = {
      val state = event.asInstanceOf[AsyncContextEvent].getHttpChannelState
      record(state.getBaseRequest, state.getServletResponse)
    }
    override def onStartAsync(event: AsyncEvent): Unit = ()
    override def onTimeout(event: AsyncEvent): Unit = ()
  }

  def record(req: Request, response: ServletResponse): Unit

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
      } else if (state.isInitial) record(request, httpResponse)
    }
  }

  protected def responseCode(request: Request): Int = request.getResponse.getStatus / 100
}

class MetricsHandler(statsdClient: MetricsStatsdClient, handler: AbstractHandler, ignore: HttpServletRequest => Boolean = isInternal) extends BaseMetricsHandler(statsdClient, handler, ignore) {

  def record(req: Request, response: ServletResponse): Unit = {
    statsdClient.incrementCounter(s"webapp.responses.${responseCode(req)}xx")
    val duration = System.currentTimeMillis - req.getTimeStamp
    statsdClient.recordExecutionTime("webapp.responsetime", duration)
  }
}

class RouteMetricsHandler(routes: Routes, statsdClient: MetricsStatsdClient, handler: AbstractHandler, ignore: HttpServletRequest => Boolean = isInternal) extends BaseMetricsHandler(statsdClient, handler, ignore) {

  def record(req: Request, response: ServletResponse) {
    val routeName: Option[String] = routes.matching(req, response.asInstanceOf[HttpServletResponse]) match {
      case Right(PathParamsRoute(_, path, _)) => Some(path)
      case Right(RegexRoute(_, pattern, _)) => Some(pattern.toString())
      case _ => None
    }

    routeName.foreach { pattern =>
      statsdClient.incrementCounter(s"webapp.$pattern.responses.${responseCode(req)}xx")
      val duration = System.currentTimeMillis - req.getTimeStamp
      statsdClient.recordExecutionTime(s"webapp.$pattern.responsetime", duration)
    }
  }
}