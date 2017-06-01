package com.springer.samatra.extras.testing

import java.net.URLEncoder.encode
import java.time.{LocalDate, ZoneOffset}
import javax.servlet.http.{Cookie, HttpServletRequest, HttpServletResponse}

import com.springer.samatra.extras.{RouteAndContext, RoutePrinting}
import com.springer.samatra.extras.responses.JsonResponses.{JsonHttpResp, JsonResponse}
import com.springer.samatra.extras.responses.XmlResponses.XmlResp
import com.springer.samatra.extras.responses.{MustacheRenderer, TemplateRenderer, TemplateResponse}
import com.springer.samatra.extras.testing.SamatraControllerTestHelpers._
import com.springer.samatra.routing.Routings.{Controller, Routes}
import com.springer.samatra.routing.StandardResponses._
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class ExampleTest extends FunSpec with ScalaFutures {

  implicit val renderer: TemplateRenderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)

  val routes: Routes = new Controller {

    import com.springer.samatra.extras.responses.XmlResponses.fromXmlResponse
    import com.springer.samatra.routing.FutureResponses.Implicits.fromFuture
    import com.springer.samatra.routing.StandardResponses.Implicits.fromString

    import scala.concurrent.ExecutionContext.Implicits.global

    put("/put") { req =>
      Future {
        Redirect(req.queryStringParamValue("to"))
      }
    }

    get("/templated/:foo") { req =>
      Future {
        TemplateResponse("foo", Map("foo" -> req.captured("foo")) ++ req.queryStringMap.mapValues(_.head))
      }
    }

    get("/json/:foo") { req =>
      JsonResponse(Map("foo" -> req.captured("foo")))
    }

    get("/xml/:foo") { req =>
      <hi>{req.captured("foo")}</hi>
    }

    get("/request-response") { req =>
      (_: HttpServletRequest, resp: HttpServletResponse) => {
        resp.setDateHeader("Date", LocalDate.of(2017, 5, 18).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli)
        resp.setStatus(200)
        resp.getWriter.print("sam")
      }
    }

    get("/hello/:name") { req =>
      Future {
        WithCookies(Seq(AddCookie("cookie", req.cookie("cookie").get))) {
          WithHeaders("a" -> "b") {
            req.captured("name")
          }
        }
      }
    }
  }

  new RoutePrinting with RouteAndContext {
    override def routesWithContext: Seq[(String, Routes)] = Seq("" -> routes)
  }.printRoutesTo(System.out)

  describe("An example of unit testing controllers") {

    it("should put") {
      whenReady(routes.put(
        "/put",
        body = encode("to=/xml/hi", "UTF-8").getBytes,
        headers = Map("Content-Type" -> Seq("application/x-www-form-urlencoded")))) { result =>

        val resp = result.run()
        resp.statusCode shouldBe 302
        resp.header("Location") shouldBe Some("/xml/hi")
      }
    }

    it("should test with helper methods") {
      whenReady(routes.get("/request-response")) { result =>
        val resp = result.run()

        resp.statusCode shouldBe 200
        resp.header("Date") shouldBe Some("Thu, 18 05 2017 12:00:00 GMT")
        resp.outputAsString shouldBe "sam"
      }
    }

    it("should test future string") {
      whenReady(routes.get("/hello/sam", cookies = Seq(new Cookie("cookie", "expectedValue")))) { result =>
        result shouldBe WithCookies(Seq(AddCookie("cookie", "expectedValue"))) {
          WithHeaders("a" -> "b") {
            StringResp("sam")
          }
        }
      }
    }

    it("should test json") {
      whenReady(routes.get("/json/sam")) { result =>
        result shouldBe JsonHttpResp(JsonResponse(Map("foo" -> "sam")))
        val run = result.run()

        run.statusCode shouldBe 200
      }
    }

    it("should test xml") {
      whenReady(routes.get("/xml/sam")) { result =>
        result shouldBe XmlResp("<hi>sam</hi>")
      }
    }

    it("returns 404 on no match") {
      whenReady(routes.get("/nomatch")) { result =>
        result shouldBe Halt(404)
      }
    }

    it("returns 405 on on MethodNotAllowed") {
      whenReady(routes.put("/xml/sam", body = "body".getBytes)) { result =>
        result shouldBe WithHeaders("Allow" -> "GET, HEAD") {
          Halt(405)
        }
      }
    }

    it("blows up on no match") {
      whenReady(routes.get("/nomatch")) { result =>
        result shouldBe Halt(404)
      }
    }

    it("should test mustache") {
      whenReady(routes.get("/templated/hi?v=1")) { result =>
        result shouldBe TemplateResponse("foo", Map("foo" -> "hi", "v" -> "1"))
        result.run().outputAsString shouldBe "<html>This is a test. Foo: hi</html>"
      }
    }
  }
}