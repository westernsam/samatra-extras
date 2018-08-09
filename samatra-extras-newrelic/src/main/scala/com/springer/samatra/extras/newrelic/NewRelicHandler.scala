package com.springer.samatra.extras.newrelic

import com.newrelic.api.agent.{NewRelic, TransactionNamePriority}
import com.springer.samatra.extras.core.jetty.{RouteAndContext, WebappContextHandler}
import com.springer.samatra.routing.Routings.{PathParamsRoute, RegexRoute}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.{AbstractHandler, HandlerWrapper}

object NewRelicHandler {
  def apply(wac: WebappContextHandler) = new NewRelicHandler(wac, wac)
}

class NewRelicHandler(routesWithContext: RouteAndContext, handler: AbstractHandler) extends HandlerWrapper {

  setHandler(handler)

  override def handle(target: String, req: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val servletPath = req.getServletPath
    val contextPath = req.getContextPath
    try {
      val routeName: Option[(String, String)] = routesWithContext.routesWithContext.flatMap { case (c, r) =>
        req.setServletPath(c)
        req.setContextPath(routesWithContext.getContextPath)
        val routeName: Option[String] = r.matching(req, response.asInstanceOf[HttpServletResponse]) match {
          case Right(PathParamsRoute(_, path, _)) => Some(path)
          case Right(RegexRoute(_, pattern, _)) => Some(pattern.toString())
          case _ => None
        }

        routeName.map(c -> _)
      }.headOption

      routeName.foreach { case (c, r) =>
        NewRelic.getAgent.getTransaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Samatra", routesWithContext.getContextPath, c, r)
      }

    } finally {
      req.setContextPath(contextPath)
      req.setServletPath(servletPath)
    }

    super.handle(target, req, request, response)
  }
}
