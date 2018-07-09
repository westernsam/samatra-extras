package com.springer.samatra.extras.routeprinting

import java.io.StringWriter

import com.springer.samatra.extras.core.jetty.RouteAndContext
import com.springer.samatra.routing.Routings
import com.springer.samatra.routing.Routings.{AggregateRoutes, Controller}
import org.scalatest.FunSpec
import com.springer.samatra.routing.StandardResponses.Implicits.fromString
import org.scalatest.Matchers._

class PrintRoutesTest extends FunSpec {

  class UnderTest extends Controller {
    get("/abc") { _ => "" }
    post("/cba") { _ => "" }
  }

  it("prints routes") {
    val out: Appendable = new StringWriter()

    new RouteAndContext with RoutePrinting {
      override def routesWithContext: Seq[(String, Routings.Routes)] = Seq("/context" -> new AggregateRoutes(new UnderTest))
    }.printRoutesTo(out)

    out.toString.trim shouldBe
      """GET    /context/abc                     -> com.springer.samatra.extras.routeprinting (PrintRoutesTest$UnderTest.scala:15)
        |POST   /context/cba                     -> com.springer.samatra.extras.routeprinting (PrintRoutesTest$UnderTest.scala:16)""".stripMargin
  }
}
