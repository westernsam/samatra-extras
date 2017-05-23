package com.springer.samatra.extras.testing

import java.io.Reader
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.Date
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.springer.samatra.extras.responses.JsonResponses.{JsonHttpResp, JsonResponse}
import com.springer.samatra.extras.responses.XmlResponses.XmlResp
import com.springer.samatra.extras.responses.{MustacheRenderer, TemplateRenderer, TemplateResponse, ViewRenderingError}
import com.springer.samatra.routing.Routings.Controller
import com.springer.samatra.routing.StandardResponses._
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import SamatraControllerTestHelpers._

import scala.collection.{immutable, mutable}
import scala.concurrent.Future

class ExampleTest extends FunSpec with ScalaFutures {

  implicit val renderer: TemplateRenderer = new MustacheRenderer(Map.empty, new MustacheRenderer.ClasspathTemplateLoader("."), false)

  val controllerUnderTest = new Controller {

    import scala.concurrent.ExecutionContext.Implicits.global
    import com.springer.samatra.routing.StandardResponses.Implicits.fromString
    import com.springer.samatra.routing.FutureResponses.Implicits.fromFuture
    import com.springer.samatra.extras.responses.XmlResponses.fromXmlResponse

    get("/templated/:foo") { req =>
      Future {
        TemplateResponse("foo", Map("foo" -> req.captured("foo")))
      }
    }

    get("/json/:foo") { req =>
      JsonResponse(Map("foo" -> req.captured("foo")))
    }

    get("/xml/:foo") { req =>
      <hi>{req.captured("foo")}</hi>
    }

    get("/request-response") { req =>
      (req:HttpServletRequest, resp:HttpServletResponse) => {
        resp.setDateHeader("Date", LocalDate.of(2017, 5, 18).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli)
        resp.setStatus(200)
        resp.getWriter.print("sam")
      }
    }

    get("/hello/:name") { req =>
      Future {
        WithCookies(Seq(AddCookie("cookie", "cookieVal"))) {
          WithHeaders("a" -> "b") {
            req.captured("name")
          }
        }
      }
    }
  }

  describe("An example of unit testing controllers") {

    it("should test future string") {
      whenReady(get(controllerUnderTest)("/hello/sam")) { result =>
        result shouldBe WithCookies(Seq(AddCookie("cookie", "cookieVal"))) {
          WithHeaders("a" -> "b") {
            StringResp("sam")
          }
        }
      }
    }

    it("should test with helper methods") {
      whenReady(get(controllerUnderTest)("/request-response")) { result =>
        result.statusCode shouldBe 200
        result.headers.get("Date") shouldBe Some(List("Thu, 18 05 2017 01:00:00 GMT"))
        result.outputAsString shouldBe "sam"
      }
    }

    it("should test json") {
      whenReady(get(controllerUnderTest)("/json/sam")) { result =>
        result shouldBe JsonHttpResp(JsonResponse(Map("foo" -> "sam")))
      }
    }

    it("should test xml") {
      whenReady(get(controllerUnderTest)("/xml/sam")) { result =>
        result shouldBe XmlResp("<hi>sam</hi>")
      }
    }

    it("returns 404 on no match") {
      whenReady(get(controllerUnderTest)("/nomatch")) { result =>
        result shouldBe Halt(404)
      }
    }

    it("returns 405 on on MethodNotAllowed") {
      whenReady(put(controllerUnderTest)("/xml/sam", body = "body".getBytes)) { result =>
        result shouldBe WithHeaders("Allow" -> "GET, HEAD") {
          Halt(405)
        }
      }
    }

    it("blows up on no match") {
      whenReady(get(controllerUnderTest)("/nomatch")) { result =>
        result shouldBe Halt(404)
      }
    }

    it("should test mustache") {
      whenReady(get(controllerUnderTest)("/templated/hi")) { result =>
        result shouldBe TemplateResponse("foo", Map("foo" -> "hi"))
      }
    }
  }
}
