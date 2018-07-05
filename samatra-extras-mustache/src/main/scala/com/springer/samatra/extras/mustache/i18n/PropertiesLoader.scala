package com.springer.samatra.extras.mustache.i18n

import java.util.Properties

import scala.collection.JavaConverters._

trait PropertiesLoader {
  def load : Map[String, String]
}

class ClasspathPropertiesLoader(path: String) extends PropertiesLoader {
  val load: Map[String, String] = {
    val properties = new Properties()
    properties.load(getClass.getResourceAsStream(path))
    properties.asScala.toMap
  }
}

class InMemoryPropertiesLoader(map: Map[String, String]) extends PropertiesLoader {
  override def load: Map[String, String] = map
}
