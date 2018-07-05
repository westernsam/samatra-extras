package com.springer.samatra.extras.core

import com.springer.samatra.routing.Routings.Controller
import com.springer.samatra.routing.StandardResponses.Implicits._

class NoRobotsController(host: String) extends Controller {

  get("/robots.txt") { _ =>
    s"""User-agent: *
        |Disallow: /
        |
      |Host: $host
        |""".stripMargin
  }

}
