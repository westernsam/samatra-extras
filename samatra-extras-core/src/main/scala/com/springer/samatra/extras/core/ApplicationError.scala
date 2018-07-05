package com.springer.samatra.extras.core

case class ApplicationError(message: AnyRef) extends Exception(message.toString)
