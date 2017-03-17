package com.springer.samatra.extras.metrics

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.springer.samatra.extras.Logger
import com.timgroup.statsd.StatsDClient

class JvmMetricsCollectorStatsDService(jvmMetricsCollector: JvmMetricsCollector, statsDClient: StatsDClient, instance: String) extends Logger {
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor

  private def recordJvmStats() {
    try {
      jvmMetricsCollector.jvmGauges.foreach {
        case (aspect: String, value: java.lang.Long) => statsDClient.gauge(qualify(aspect), value); log.debug(s"${qualify(aspect)}, $value")
        case (aspect: String, value: java.lang.Double) => statsDClient.gauge(qualify(aspect), value); log.debug(s"${qualify(aspect)}, $value")
        case (aspect: String, value: Number) => log.error(s"Unable to quantify ${qualify(aspect)} of type ${value.getClass.getSimpleName}")
      }
      jvmMetricsCollector.jvmCounters.foreach {
        case (aspect: String, value: Number) => statsDClient.count(qualify(aspect), value.longValue()); log.debug(s"${qualify(aspect)}, $value")
      }
    }
    catch {
      case e: Exception => log.error("Error collecting jvm stats", Some(e))
    }
  }

  def initialise() {
    log.info("initialising jvm stats recording")
    scheduler.scheduleAtFixedRate(() => recordJvmStats(), 0, 10, TimeUnit.SECONDS)
  }

  private def qualify(aspect: String): String = "jvm." + instance + "." + aspect
}
