package com.springer.samatra.extras.core.jetty

import java.util
import com.springer.samatra.routing.Routings.Routes

import javax.servlet.{DispatcherType, Filter}
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler, ServletHolder}

import scala.collection.mutable

class WebappContextHandler extends ServletContextHandler with RouteAndContext {
  def routesWithContext: Seq[(String, Routes)] = routesWithContextBuffer.toSeq

  protected val routesWithContextBuffer = new mutable.ListBuffer[(String, Routes)]()

  def addFilter(path: String = "/*", filter: Filter): this.type = {
    super.addFilter(new FilterHolder(filter), path, util.EnumSet.allOf(classOf[DispatcherType]))
    this
  }

  def addRoutes(path: String, routes: Routes*): this.type = {
    routes.foreach(r => routesWithContextBuffer.append(path.replaceAll("/\\*$", "") -> r))
    super.addServlet(new ServletHolder(Routes(routes: _*)), path)
    this
  }
}
