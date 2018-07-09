package com.springer.samatra.extras.core.jetty

import java.util

import com.springer.samatra.routing.Routings.Routes
import javax.servlet.{DispatcherType, Filter}
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler, ServletHolder}

import scala.collection.mutable

class WebappContextHandler extends ServletContextHandler with RouteAndContext {
  val routesWithContext: mutable.ArrayBuffer[(String, Routes)] = new mutable.ArrayBuffer[(String, Routes)]()

  def addFilter(filter: Filter, path: String = "/*"): this.type = {
    super.addFilter(new FilterHolder(filter), path, util.EnumSet.allOf(classOf[DispatcherType]))
    this
  }

  def addRoutes(path: String, routes: Routes*): this.type = {
    routes.foreach(r => routesWithContext.append(path.replaceAll("/\\*$", "") -> r))
    super.addServlet(new ServletHolder(Routes(routes: _*)), path)
    this
  }
}
