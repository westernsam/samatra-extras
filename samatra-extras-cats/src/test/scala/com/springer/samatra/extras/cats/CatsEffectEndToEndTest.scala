package com.springer.samatra.extras.cats

import cats.effect.IO
import com.springer.samatra.extras.cats.IoResponses.{IoResponse, Timeout, fromIOWithTimeout}
import com.springer.samatra.routing.Routings.{Controller, HttpResp, Routes}
import com.springer.samatra.routing.StandardResponses.Implicits.{fromFile, fromString}
import io.netty.handler.codec.http.DefaultHttpHeaders
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.Response
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.server.{Connector, Server, ServerConnector}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}
import cats.effect.unsafe.implicits.global
import com.springer.samatra.routing.StandardResponses.{AddCookie, Halt, Redirect, WithCookies, WithHeaders}

import java.nio.file.Paths

class CatsEffectEndToEndTest extends AnyFunSpec with BeforeAndAfterAll {

  private val server = new Server() {
    addConnector(new ServerConnector(this) {
      setPort(0)
    })
  }

  private val handler = new GzipHandler()
  handler.setHandler(new ServletContextHandler() {
    setContextPath("/test")

    addServlet(new ServletHolder(
      Routes(new Controller {

        get("/relative") { req =>
          IO.sleep(100.millis).map { _ =>
            req.relativePath
          }
        }

        get("/morethanone/:type") { req =>
          IO[HttpResp] {
            req.captured("type") match {
              case "redirect" => Redirect("/getandpost")
              case "string" => "String"
              case "Error" => Halt(500, Some(new RuntimeException("error")))
              case "NotFound" => Halt(404)
              case "file" => WithHeaders("Content-Type" -> "application/xml") {
                Paths.get("build.sbt")
              }
              case "headers" => WithHeaders("hi" -> "there")("body")
              case "cookies" =>
                WithCookies(AddCookie("cookie", "tasty"))("body")
              case "securedcookies" =>
                WithCookies(AddCookie("cookie", "tasty", httpOnly = true))("body")
            }
          }
        }

        //use explicits
        get("/timeout") { _ =>
          fromIOWithTimeout(IO.sleep(100.second).map { _ =>
            "unexpceted"
          })(Timeout(200))
        }

      })), "/cats/*")
  })

  server.setHandler(handler)

  it("has correct relative path after async") {
    get("/test/cats/relative").getResponseBody shouldBe "/relative"
  }

  it("HEAD should return 200, 302, 404 and 500 error codes") {
    head("/test/cats/morethanone/Error").getStatusCode shouldBe 500
    head("/test/cats/morethanone/NotFound").getStatusCode shouldBe 404
    head("/test/cats/morethanone/redirect").getStatusCode shouldBe 302
    head("/test/cats/morethanone/string").getStatusCode shouldBe 200
    head("/test/cats/morethanone/headers").getHeader("hi") shouldBe "there"

    val cookies = head("/test/cats/morethanone/cookies").getCookies
    val cookie = cookies.asScala.collectFirst {
      case c if c.name() == "cookie" => c.value()
    }

    cookie shouldBe Some("tasty")
  }

  it("can timeout") {
      val res = get("/test/cats/timeout")
      res.getStatusCode shouldBe 500
  }


  def head(path: String): Response = asyncHttpClient.prepareHead(s"$host$path").execute().get()

  def get(path: String, headers: Map[String, Seq[String]] = Map.empty): Response = {
    val hs = new DefaultHttpHeaders()
    headers.foreach { case (k, v) => hs.add(k, v.asJava) }

    asyncHttpClient.prepareGet(s"$host$path")
      .setHeaders(hs)
      .execute().get()
  }

  override protected def beforeAll(): Unit = {
    server.start()
    val connectors: Array[Connector] = server.getConnectors
    val port: Int = connectors(0).asInstanceOf[ServerConnector].getLocalPort

    host = s"http://localhost:$port"
  }

  override protected def afterAll(): Unit = server.stop()

  var host: String = _
}
