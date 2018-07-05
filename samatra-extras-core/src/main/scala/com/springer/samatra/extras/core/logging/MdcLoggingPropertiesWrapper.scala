package com.springer.samatra.extras.core.logging

trait MdcLoggingPropertiesWrapper {
  def withLoggingProperty(properties: (String, String)*)(block: => Unit) {
    properties.foreach { case (key, value) => org.slf4j.MDC.put(key, value) }
    try block finally properties.foreach { case (key, _) => org.slf4j.MDC.remove(key) }
  }
}
