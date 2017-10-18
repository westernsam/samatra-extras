package com.springer.samatra.extras.metrics

import javax.servlet.http.HttpServletRequest

import com.springer.samatra.routing.Routings.Routes
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler


class GzippedMetricsEnabledHandler(rest: AbstractHandler, statsDClient: MetricsStatsdClient,
                                   ignoreInMetrics: HttpServletRequest => Boolean = MetricsHandler.isInternal) extends GzipHandler {
  //defaults to *MS6.0* - and therefore adds Vary: user-agent header
  setExcludedAgentPatterns()
  setHandler(new MetricsHandler(statsDClient, rest, ignoreInMetrics))
}

class GzippedRoutesMetricsEnabledHandler(routes: Routes, rest: AbstractHandler, statsDClient: MetricsStatsdClient,
                                   ignoreInMetrics: HttpServletRequest => Boolean = MetricsHandler.isInternal) extends GzipHandler {
  //defaults to *MS6.0* - and therefore adds Vary: user-agent header
  setExcludedAgentPatterns()
  setHandler(new RouteMetricsHandler(routes, statsDClient, new MetricsHandler(statsDClient, rest, ignoreInMetrics)))
}