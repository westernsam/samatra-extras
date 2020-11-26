package com.springer.samatra.extras.websockets

import java.io.StringWriter
import com.springer.samatra.extras.core.jetty.RouteAndContext
import com.springer.samatra.routing.Routings._
import com.springer.samatra.routing.StandardResponses.Implicits.fromString
import com.springer.samatra.routing.{Request, Routings}
import com.springer.samatra.websockets.WsRoutings.{WSController, WriteOnly}
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funspec.AnyFunSpec

class WsPrintRoutesTest extends AnyFunSpec {

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
        Seq("/servlet" ->
          new AggregateRoutes(
            new UnderTest,
            new WebSocketRoutes(ws.routes.toSeq.map(r => PathParamsRoute(GET, r.path, r.socket.asInstanceOf[Function[Request, HttpResp]])), ws.getClass))
        )
      }
      override def getContextPath: String = "/context"
    }.printRoutesTo(out)

    out.toString.trim shouldBe
      """GET    /context/servlet/abc             -> com.springer.samatra.extras.websockets (WsPrintRoutesTest$UnderTest.scala:15)
        |POST   /context/servlet/cba             -> com.springer.samatra.extras.websockets (WsPrintRoutesTest$UnderTest.scala:16)
        |WS     /context/servlet/ws              -> com.springer.samatra.extras.websockets (WsPrintRoutesTest$Ws.scala:20)""".stripMargin
  }
}
