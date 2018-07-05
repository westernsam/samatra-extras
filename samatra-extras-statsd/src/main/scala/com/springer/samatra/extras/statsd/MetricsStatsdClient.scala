package com.springer.samatra.extras.statsd

import com.springer.samatra.extras.core.logging.Logger
import com.timgroup.statsd.StatsDClient


trait MetricsStatsdClient {
  def incrementCounter(name: String)
  def gauge(name: String, value: Long)
  def recordExecutionTime(name: String, duration: Long)
}

object MetricsStatsdClient extends Logger {
  def newStatsDClient(statsd: StatsDClient, env: String): MetricsStatsdClient = new MetricsStatsdClient {
    override def incrementCounter(name: String): Unit =
      if (!env.equals("LOCAL")) statsd.incrementCounter(name)
      else log.info(s"Incrementing metrics counter $name")

    override def gauge(name: String, value: Long): Unit =
      if (!env.equals("LOCAL")) statsd.gauge(name, value)
      else log.info(s"Recording gauge metric ($name, $value)")

    override def recordExecutionTime(name: String, duration: Long): Unit =
      if (!env.equals("LOCAL")) statsd.recordExecutionTime(name, duration)
      else log.info(s"Recording execution time metric ($name, $duration)")
  }
}