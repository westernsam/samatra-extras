package com.springer.samatra.extras

import java.io.{PrintWriter, StringWriter}


trait PrintStackTrace {
  val exception: Throwable

  lazy val printStackTrace = {
    val writer: StringWriter = new StringWriter()
    exception.printStackTrace(new PrintWriter(writer))
    writer.toString
  }

  override def toString: String = s"${getClass.getName}(\n$printStackTrace)"
}




