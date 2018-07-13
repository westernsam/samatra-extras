package com.springer.samatra.extras.newrelic

import com.newrelic.api.agent.{NewRelic, TransactionNamePriority}
import com.springer.samatra.extras.core.jetty.WebappContextHandler
import com.springer.samatra.routing.Routings.{PathParamsRoute, RegexRoute, Routes}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.{AbstractHandler, HandlerWrapper}

object NewRelicHandler {
  def apply(wac: WebappContextHandler) = new NewRelicHandler(wac.routesWithContext, wac)
}

class NewRelicHandler(routesWithContext: Seq[(String, Routes)], handler: AbstractHandler) extends HandlerWrapper {

  setHandler(handler)

  override def handle(target: String, req: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val servletPath = req.getServletPath
    try {
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
        NewRelic.getAgent.getTransaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Samatra", c, r)
      }

    } finally {
      req.setServletPath(servletPath)
    }

    super.handle(target, req, request, response)
  }
}
