package com.springer.samatra.extras.websockets

import com.springer.samatra.extras.WebappContextHandler
import com.springer.samatra.routing.Request
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