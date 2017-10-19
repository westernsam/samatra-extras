package com.springer.samatra.extras.metrics

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{AsyncEvent, AsyncListener, ServletResponse}

import com.springer.samatra.extras.metrics.MetricsHandler._
import com.springer.samatra.routing.Routings.{PathParamsRoute, RegexRoute, Routes}
import org.eclipse.jetty.server.handler.{AbstractHandler, HandlerWrapper}
import org.eclipse.jetty.server.{AsyncContextEvent, HttpChannelState, Request}

object MetricsHandler {
  def isInternal(request: HttpServletRequest): Boolean = request.getRequestURI.startsWith("/internal")
  def responseCode(status:Int): String = s"${status / 100}xx"
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
}

class MetricsHandler(statsdClient: MetricsStatsdClient, handler: AbstractHandler, ignore: HttpServletRequest => Boolean = isInternal) extends BaseMetricsHandler(statsdClient, handler, ignore) {

  def record(req: Request, response: ServletResponse): Unit = {
    statsdClient.incrementCounter("webapp.requests")
    statsdClient.incrementCounter(s"webapp.responses.${responseCode(req.getResponse.getStatus)}")
    val duration = System.currentTimeMillis - req.getTimeStamp
    statsdClient.recordExecutionTime("webapp.responsetime", duration)
  }
}


class RouteMetricsHandler(routesWithContext: Seq[(String, Routes)], statsdClient: MetricsStatsdClient, handler: AbstractHandler, ignore: HttpServletRequest => Boolean = isInternal, pathTransformer: String => String = identity) extends BaseMetricsHandler(statsdClient, handler, ignore) {

  def record(req: Request, response: ServletResponse) {

    val routeName: Option[(String, String)] = routesWithContext.flatMap { case (c, r) =>

      req.setServletPath(c)

      val routeName: Option[String] = r.matching(req, response.asInstanceOf[HttpServletResponse]) match {
        case Right(PathParamsRoute(_, path, _)) => Some(path)
        case Right(RegexRoute(_, pattern, _)) => Some(pattern.toString())
        case _ => None
      }

      routeName.map(c -> _)
    }.headOption

    routeName.foreach { case (c, r) =>
      val path = pathTransformer(s"$c$r")
      statsdClient.incrementCounter(s"webapp.$path.responses.${responseCode(req.getResponse.getStatus)}")
      val duration = System.currentTimeMillis - req.getTimeStamp
      statsdClient.recordExecutionTime(s"webapp.$path.responsetime", duration)
    }
  }
}