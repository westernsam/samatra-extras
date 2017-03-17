package com.springer.samatra.extras.metrics

import javax.servlet.http.HttpServletRequest

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler


class GzippedMetricsEnabledHandler(rest: AbstractHandler, statsDClient: MetricsStatsdClient,
                                   ignoreInMetrics: HttpServletRequest => Boolean = MetricsHandler.isInternal) extends GzipHandler {
  //defaults to *MS6.0* - and therefore adds Vary: user-agent header
  setExcludedAgentPatterns()
  setHandler(new MetricsHandler(statsDClient, rest, ignoreInMetrics))
}
