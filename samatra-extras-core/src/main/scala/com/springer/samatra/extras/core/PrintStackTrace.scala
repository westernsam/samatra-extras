package com.springer.samatra.extras.core

import java.io.{PrintWriter, StringWriter}


trait PrintStackTrace {
  val exception: Throwable

  lazy val printStackTrace: String = {
    val writer: StringWriter = new StringWriter()
    exception.printStackTrace(new PrintWriter(writer))
    writer.toString
  }

  override def toString: String = s"${getClass.getName}(\n$printStackTrace)"
}




