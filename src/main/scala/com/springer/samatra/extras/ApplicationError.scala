package com.springer.samatra.extras

case class ApplicationError(message: AnyRef) extends Exception(message.toString)
