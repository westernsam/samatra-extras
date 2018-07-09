package com.springer.samatra.extras.websockets

import java.io.StringWriter

import com.springer.samatra.extras.core.jetty.RouteAndContext
import com.springer.samatra.routing.Routings._
import com.springer.samatra.routing.StandardResponses.Implicits.fromString
import com.springer.samatra.routing.{Request, Routings}
import com.springer.samatra.websockets.WsRoutings.{WSController, WriteOnly}
import org.scalatest.FunSpec
import org.scalatest.Matchers._

class WsPrintRoutesTest extends FunSpec {

  class UnderTest extends Controller {
    get("/abc") { _ => "" }
    post("/cba") { _ => "" }
  }

  class Ws extends WSController {
    mount("/ws") { _ => new WriteOnly {} }
  }

  it("prints routes") {
    val out: Appendable = new StringWriter()

    new RouteAndContext with WSRoutePrinting {
      override def routesWithContext: Seq[(String, Routings.Routes)] = {
        val ws = new Ws()
        Seq("/context" ->
          new AggregateRoutes(
            new UnderTest,
            new WebSocketRoutes(ws.routes.map(r => PathParamsRoute(GET, r.path, r.socket.asInstanceOf[Function[Request, HttpResp]])), ws.getClass))
        )
      }
    }.printRoutesTo(out)

    out.toString.trim shouldBe
      """GET    /context/abc                     -> com.springer.samatra.extras.websockets (WsPrintRoutesTest$UnderTest.scala:16)
        |POST   /context/cba                     -> com.springer.samatra.extras.websockets (WsPrintRoutesTest$UnderTest.scala:17)
        |WS     /context/ws                      -> com.springer.samatra.extras.websockets (WsPrintRoutesTest$Ws.scala:21)""".stripMargin
  }
}
