package com.springer.samatra.extras.testing

import java.io.Reader

import com.springer.samatra.extras.responses.JsonResponses.{JsonHttpResp, JsonResponse}
import com.springer.samatra.extras.responses.XmlResponses.XmlResp
import com.springer.samatra.extras.responses.{TemplateRenderer, TemplateResponse, ViewRenderingError}
import com.springer.samatra.routing.Routings.Controller
import com.springer.samatra.routing.StandardResponses.{AddCookie, StringResp, WithCookies, WithHeaders}
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import SamatraControllerTestHelpers.get

import scala.concurrent.Future

class ExampleTest extends FunSpec with ScalaFutures {

  implicit val fakeTemplates : TemplateRenderer = new TemplateRenderer {
    override def rendered(viewName: String, model: Map[String, Any]): Either[ViewRenderingError, String] = Right(viewName)
    override def rendered(reader: Reader, model: Map[String, Any]): Either[ViewRenderingError, String] = Right("reader")
  }

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

  it("should test future string") {
    whenReady(get(controllerUnderTest)("/hello/sam")) { result =>
      result shouldBe WithCookies(Seq(AddCookie("cookie", "cookieVal"))) {
        WithHeaders("a" -> "b") {
          StringResp("sam")
        }
      }
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

  it("should test mustache") {
    whenReady(get(controllerUnderTest)("/templated/hi")) { result =>
      result shouldBe TemplateResponse("foo", Map("foo" -> "hi"))
    }
  }
}
