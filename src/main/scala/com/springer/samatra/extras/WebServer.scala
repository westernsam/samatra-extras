package com.springer.samatra.extras

import java.lang.management.ManagementFactory

import org.eclipse.jetty.jmx.MBeanContainer
import org.eclipse.jetty.server.handler._
import org.eclipse.jetty.server.{Connector, Server, ServerConnector}
import org.eclipse.jetty.util.thread.QueuedThreadPool

object WebServer {
  def standardConfig(connector:ServerConnector) : Unit = {
    /*
    * Reduce amount of time socket spend in TIME_WAIT after server initiates close
    *
    * See:
    *   - http://stackoverflow.com/questions/3757289/tcp-option-so-linger-zero-when-its-required
    * */
    connector.setSoLingerTime(1000)
    connector.setAcceptQueueSize(25)
  }
}

class WebServer(port: Int = 0, configureConnector: (ServerConnector) => Unit = WebServer.standardConfig, maxJettyThreads: Int = 25) extends Logger {

  private val server = new Server(new QueuedThreadPool(maxJettyThreads))
  val connector: ServerConnector = new ServerConnector(server)
  connector.setPort(port)
  configureConnector(connector)

  server.setConnectors(Array[Connector](connector))

  private val handlers = new HandlerCollection()
  server.setHandler(handlers)

  val mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer)
  server.addEventListener(mbContainer)
  server.addBean(mbContainer)

  def addErrorHandler(errorHandler: ErrorHandler): WebServer = {
    server.addBean(errorHandler, false)
    this
  }

  def addHandler(handler: AbstractHandler): WebServer = {
    handlers.addHandler(handler)
    this
  }

  def start(): Unit = {
    server.start()
    log.info("Jetty Server started")
  }

  def stop(): Unit = {
    server.stop()
    log.info("Jetty Server stopped")
  }

  def isRunning: Boolean = server.isRunning

  def startAndWait(): Unit = {
    start()
    server.join()
  }

  def httpPort: Int =
    if (!server.isRunning)
      throw new IllegalStateException("Server not running")
    else {
      val connectors: Array[Connector] = server.getConnectors
      connectors(0).asInstanceOf[ServerConnector].getLocalPort
    }
}