package com.springer.samatra.extras.core.jetty

import com.springer.samatra.routing.Routings.Routes

trait RouteAndContext {
  def routesWithContext: Seq[(String, Routes)]
}
