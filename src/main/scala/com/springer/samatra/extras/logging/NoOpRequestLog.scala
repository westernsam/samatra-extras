package com.springer.samatra.extras.logging

import org.eclipse.jetty.server.{Request, RequestLog, Response}

class NoOpRequestLog extends RequestLog {
  override def log(request: Request, response: Response): Unit = ()
}
