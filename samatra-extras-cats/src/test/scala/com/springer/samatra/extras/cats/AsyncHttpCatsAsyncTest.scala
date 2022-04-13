package com.springer.samatra.extras.cats

import cats.effect.IO
import com.springer.samatra.extras.cats.AsyncHttpCatsAsycnExtensions.ListenableFutureIoOps
import com.springer.samatra.routing.Routings.{Controller, Routes}
import com.springer.samatra.routing.StandardResponses.Implicits.fromString
import org.asynchttpclient.Dsl.asyncHttpClient
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.server.{Connector, Server, ServerConnector}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class AsyncHttpCatsAsyncTest extends AnyFunSpec with BeforeAndAfterAll {

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

        get("/test") { _ =>
            "test"
          }
      })), "/cats/*")
  })

  server.setHandler(handler)

  it("can lift an async http client to Async") {
    import cats.effect.unsafe.implicits.global

    val io = asyncHttpClient.prepareGet(s"$host/test/cats/test").execute().toAsync[IO]

    io.unsafeRunSync().getStatusCode shouldBe 200
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
