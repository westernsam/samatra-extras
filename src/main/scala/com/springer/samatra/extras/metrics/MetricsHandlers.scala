package com.springer.samatra.extras.metrics

import javax.servlet.http.HttpServletRequest

import com.springer.samatra.extras.WebappContextHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler


object MetricsHandlers {

  def gzipAndWebMetrics(rest: WebappContextHandler, statsDClient: MetricsStatsdClient,
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
      setHandler(new RouteMetricsHandler(rest.routesWithContext, statsDClient, new MetricsHandler(statsDClient, rest, ignoreInMetrics)))
    }
  }
}