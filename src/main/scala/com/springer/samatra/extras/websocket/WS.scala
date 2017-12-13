package com.springer.samatra.extras.websocket

import java.nio.ByteBuffer
import java.util
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.springer.samatra.extras.WebappContextHandler
import com.springer.samatra.routing.Request
import com.springer.samatra.routing.Routings._
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.api
import org.eclipse.jetty.websocket.api.{CloseStatus, WebSocketListener}
import org.eclipse.jetty.websocket.servlet.{ServletUpgradeRequest, ServletUpgradeResponse, WebSocketServlet, WebSocketServletFactory}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, blocking}

object WS {
  /*
  * Todos:
  * - can this be made using javax.websocket.api only?
  * - can we make in memory testing work?
  * */
  abstract class WSController extends Routes {
    private[WS] val socks = mutable.Buffer[(String, WSSocket)]()
    val routes: mutable.Buffer[Route] = mutable.Buffer[Route]()

    trait WSSend {
      def send(msg: String): Unit
      def send(msg: Future[String])(implicit ex: ExecutionContext): Future[Unit]
      def sendBinary(msg: Array[Byte]): Unit
      def sendBinary(msg: Future[Array[Byte]])(implicit ex: ExecutionContext): Future[Unit]
      def close(code: Int, msg: String): Unit
    }

    trait WS {
      def onConnect(): Unit = ()
      def onMsg(msg: String): Unit
      def onMsg(msg: Array[Byte]): Unit = throw new UnsupportedOperationException("Binary data unsupported")
      def onEnd(code: Int): Unit = ()
      def onError(throwable: Throwable): Unit = ()
    }

    trait WriteOnly extends WS {
      final override def onMsg(msg: String): Unit = ()
      final override def onMsg(msg: Array[Byte]): Unit = ()
    }

    def mount(path: String)(ws: WSSend => WS): Unit = {
      socks.append(path -> new WSSocket(ws))
      routes.append(PathParamsRoute(GET, path, ws.asInstanceOf[Request => HttpResp]))
    }

    class WSSocket(val ws: WSSend => WS) extends WebSocketListener {
      var wsoc: WS = _
      override def onWebSocketConnect(session: api.Session): Unit = {
        wsoc = ws(new WSSend {
          override def send(msg: String): Unit = session.getRemote.sendString(msg)
          override def send(msg: Future[String])(implicit ex: ExecutionContext): Future[Unit] = {
            msg.flatMap { str =>
              val value: util.concurrent.Future[Void] = session.getRemote.sendStringByFuture(str)
              Future {
                blocking {
                  value.get
                }
              }
            }
          }
          override def sendBinary(msg: Array[Byte]): Unit = session.getRemote.sendBytes(ByteBuffer.wrap(msg))
          override def sendBinary(msg: Future[Array[Byte]])(implicit ex: ExecutionContext): Future[Unit] = {
            msg.flatMap { bytes =>
              val value: util.concurrent.Future[Void] = session.getRemote.sendBytesByFuture(ByteBuffer.wrap(bytes))
              Future {
                blocking {
                  value.get
                }
              }
            }
          }
          override def close(code: Int, msg: String): Unit = session.close(new CloseStatus(code, msg))
        })
        wsoc.onConnect()
      }
      override def onWebSocketBinary(payload: Array[Byte], offset: Int, len: Int): Unit = if (wsoc == null) throw new IllegalStateException("Connect first") else wsoc.onMsg(payload.slice(offset, len))
      override def onWebSocketClose(statusCode: Int, reason: String): Unit = if (wsoc == null) throw new IllegalStateException("Connect first") else  wsoc.onEnd(statusCode)
      override def onWebSocketText(message: String): Unit = if (wsoc == null) throw new IllegalStateException("Connect first") else wsoc.onMsg(message)
      override def onWebSocketError(cause: Throwable): Unit = if (wsoc == null) throw new IllegalStateException("Connect first") else wsoc.onError(cause)
    }
  }

  class WsRoutes(val routes: Seq[Route]) extends Routes {
    override def matching(req: HttpServletRequest, resp: HttpServletResponse): Either[Seq[Route], Route] = Left(Seq.empty)
  }

  trait WsRoutesHandler {
    self: WebappContextHandler =>
    def addWsRoutes(pathSpec: String, wSControllers: WSController*): this.type = addWsRoutes(pathSpec, None, wSControllers: _*)
    def addWsRoutes(pathSpec: String, idleTime: Option[Long], wSControllers: WSController*): this.type = {

      val wcs: Seq[(String, WSController#WSSocket)] = wSControllers.toSeq.flatMap(_.socks.toSeq)
      val routes: Seq[Route] = wSControllers.flatMap(_.routes)

      val servlet = new WebSocketServlet {
        override def configure(factory: WebSocketServletFactory): Unit = {
          idleTime.foreach(t => factory.getPolicy.setIdleTimeout(t))

          factory.setCreator((req: ServletUpgradeRequest, _: ServletUpgradeResponse) => {
            val request = Request(req.getHttpServletRequest, Map.empty, started = System.currentTimeMillis())

            val tuples: Seq[(Route, (String, WSController#WSSocket))] = routes.zip(wcs)
            tuples.find(_._1.matches(request).isDefined).map(_._2._2).orNull
          })
        }
      }

      self.routesWithContext.append("ws://(hostname)" + pathSpec.substring(0, pathSpec.length - 2) -> new AggregateRoutes(wSControllers:_*))
      self.addServlet(new ServletHolder(servlet), pathSpec)

      this
    }
  }
}
