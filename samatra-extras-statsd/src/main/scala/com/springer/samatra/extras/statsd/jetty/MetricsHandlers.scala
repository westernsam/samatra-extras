package com.springer.samatra.extras.statsd.jetty

import com.springer.samatra.extras.routeprinting.WebappContextHandler
import com.springer.samatra.extras.statsd.MetricsStatsdClient
import javax.servlet.http.HttpServletRequest
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler


object MetricsHandlers {

  def gzipAndWebMetrics(rest: AbstractHandler, statsDClient: MetricsStatsdClient,
                        ignoreInMetrics: HttpServletRequest => Boolean = MetricsHandler.isInternal): GzipHandler = {
    new GzipHandler {
      //defaults to *MS6.0* - and therefore adds Vary: user-agent header
      setExcludedAgentPatterns()
      setHandler(new MetricsHandler(statsDClient, new MetricsHandler(statsDClient, rest, ignoreInMetrics)))
    }
  }

  def gzipWebAndRouteMetrics(rest: WebappContextHandler, statsDClient: MetricsStatsdClient,
                             ignoreInMetrics: HttpServletRequest => Boolean = MetricsHandler.isInternal): GzipHandler = {
    new GzipHandler {
      //defaults to *MS6.0* - and therefore adds Vary: user-agent header
      setExcludedAgentPatterns()
      setHandler(new MetricsHandler(statsDClient, new RouteMetricsHandler(rest.routesWithContext, statsDClient, rest)))
    }
  }
}