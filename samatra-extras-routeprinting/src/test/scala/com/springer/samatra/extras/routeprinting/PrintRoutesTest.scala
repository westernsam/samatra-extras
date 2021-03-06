package com.springer.samatra.extras.routeprinting

import java.io.StringWriter
import com.springer.samatra.extras.core.jetty.RouteAndContext
import com.springer.samatra.routing.Routings
import com.springer.samatra.routing.Routings.{AggregateRoutes, Controller}
import com.springer.samatra.routing.StandardResponses.Implicits.fromString
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers._

class PrintRoutesTest extends AnyFunSpec {

  class UnderTest extends Controller {
    get("/abc") { _ => "" }
    post("/cba") { _ => "" }
  }

  it("prints routes") {
    val out: Appendable = new StringWriter()

    new RouteAndContext with RoutePrinting {
      override def routesWithContext: Seq[(String, Routings.Routes)] = Seq("/servlet" -> new AggregateRoutes(new UnderTest))
      override def getContextPath: String = "/abc"
    }.printRoutesTo(out)

    out.toString.trim shouldBe
      """GET    /abc/servlet/abc                 -> com.springer.samatra.extras.routeprinting (PrintRoutesTest$UnderTest.scala:14)
        |POST   /abc/servlet/cba                 -> com.springer.samatra.extras.routeprinting (PrintRoutesTest$UnderTest.scala:15)""".stripMargin
  }

  it("prints routes with empty context route") {
    val out: Appendable = new StringWriter()

    new RouteAndContext with RoutePrinting {
      override def routesWithContext: Seq[(String, Routings.Routes)] = Seq("/servlet" -> new AggregateRoutes(new UnderTest))
      override def getContextPath: String = "/"
    }.printRoutesTo(out)

    out.toString.trim shouldBe
      """GET    /servlet/abc                     -> com.springer.samatra.extras.routeprinting (PrintRoutesTest$UnderTest.scala:14)
        |POST   /servlet/cba                     -> com.springer.samatra.extras.routeprinting (PrintRoutesTest$UnderTest.scala:15)""".stripMargin
  }
}
