package com.springer.samatra.extras.core.logging

trait LogBackEnvConfiguredLogging {

  val fromEnv: Option[String] = scala.sys.env.get("LOGBACK_XML_PATH")
  val fromSysProperty: Option[String] = Option(System.getProperty("logback.configurationFile"))

  (fromEnv, fromSysProperty) match {
    case (Some(path), None) =>
      println(s"Loading alternative logback configuration from [$path]")
      System.setProperty("logback.configurationFile", path)
    case _ =>
  }
}
