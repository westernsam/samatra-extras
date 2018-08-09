package com.springer.samatra.extras.websockets

import com.springer.samatra.extras.core.jetty.{RouteAndContext, WebappContextHandler}
import com.springer.samatra.extras.routeprinting.RoutePrinting.RouteWithLineNumber
import com.springer.samatra.extras.routeprinting.RoutePrinting
import com.springer.samatra.routing.{Request, Routings}
import com.springer.samatra.routing.Routings._
import com.springer.samatra.websockets.WsRoutings.{WSController, WsRoutes}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer.configureContext

class WebSocketRoutes(override val routes: Seq[Route], val containingClazz: Class[_]) extends Routes

trait WsRoutesHandler {
  self: WebappContextHandler =>

  def addWsRoutes(pathSpec: String, wSControllers: WSController*): this.type = {

    val spec = pathSpec.substring(0, pathSpec.length - 2)
    if (routesWithContext.map(_._1).contains(spec)) throw new IllegalStateException(s"Multiple servlets map to path $pathSpec")

    //set this or server needs to be set on handler before creating Server Container
    self.getServletContext.setAttribute(WebSocketServerContainerInitializer.HTTPCLIENT_ATTRIBUTE, new HttpClient())
    WsRoutes(configureContext(self), pathSpec, wSControllers: _*)

    //add routes
    wSControllers.foreach { wc =>
      self.routesWithContext.append(spec ->
        //hack for printing WsRoutes when they aren't really routes
        new WebSocketRoutes(wc.routes.map(r => PathParamsRoute(GET, r.path, r.socket.asInstanceOf[Function[Request, HttpResp]])), wc.getClass))
    }

    this
  }
}

case class WSRouteWithLineNumber(r: Route, i: Option[Int]) extends RouteWithLineNumber {

  def printRoute(contextPath:String, servletPath: String = "", out: Appendable): Unit = r match {
    case PathParamsRoute(method, pattern, resp) if method != Routings.HEAD => out.append(printRoute(method, contextPath + servletPath + pattern, resp, i))
    case _ => //noop
  }

  private def printRoute(method: Routings.HttpMethod, pattern: String, resp: Request => HttpResp, ln: Option[Int]): String = {
    val clazz: Class[_ <: Request => HttpResp] = resp.getClass
    val enclosingClassName = s"${clazz.getName.split("\\$\\$")(0).split("\\.").reverse.head}"
    s"WS     ${pattern.toString.padTo(32, ' ')} -> ${clazz.getPackage.getName} ($enclosingClassName.scala:${ln.getOrElse("?")})\n"
  }
}

trait WSRoutePrinting extends RoutePrinting {
  self: RouteAndContext =>
  override def routesWithLineNumbers: Seq[(String, RoutePrinting.RouteWithLineNumber)] = {
    super.routesWithLineNumbers ++ RoutePrinting.hackOutLineNumbers(routesWithContext, { (routes, route) =>
      route match {
        case PathParamsRoute(_, pattern, _) if routes.isInstanceOf[WebSocketRoutes] => Some(WSRouteWithLineNumber(route, RoutePrinting.lineNumber(pattern, routes.asInstanceOf[WebSocketRoutes].containingClazz))) //orrible hack for web sockets
        case _ => None
      }
    })
  }
}