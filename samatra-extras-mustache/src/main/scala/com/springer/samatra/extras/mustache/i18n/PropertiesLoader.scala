package com.springer.samatra.extras.mustache.i18n

import java.util.Properties
import scala.jdk.CollectionConverters.DictionaryHasAsScala

trait PropertiesLoader {
  def load : Map[String, String]
}

class ClasspathPropertiesLoader(path: String) extends PropertiesLoader {
  val load: Map[String, String] = {
    val properties = new Properties()
    properties.load(getClass.getResourceAsStream(path))
    properties.asScala.toMap.asInstanceOf[Map[String, String]]
  }
}

class InMemoryPropertiesLoader(map: Map[String, String]) extends PropertiesLoader {
  override def load: Map[String, String] = map
}
