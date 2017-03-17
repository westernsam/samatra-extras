package com.springer.samatra.extras

import org.slf4j.{LoggerFactory, Logger => Slf4jLogger}

trait Logger extends Appendable {

  private val clazz = getClass
  val log = new LoggerWrapper(LoggerFactory.getLogger(clazz))

  override def append(csq: CharSequence): Appendable = {
    log.info(csq.toString.trim())
    this
  }

  override def append(csq: CharSequence, start: Int, end: Int): Appendable = {
    log.info(csq.subSequence(start, end).toString.trim())
    this
  }

  override def append(c: Char): Appendable = {
    log.info(c.toString.trim())
    this
  }

  sealed class LoggerWrapper(delegate: Slf4jLogger) {

    def isDebugEnabled = delegate.isDebugEnabled

    def isInfoEnabled = delegate.isInfoEnabled

    def isWarnEnabled = delegate.isWarnEnabled

    def isErrorEnabled = delegate.isErrorEnabled

    def debug(message: => String, e: Option[Throwable] = None) = if (isDebugEnabled) e match {
      case Some(ex) => delegate.debug(message, ex)
      case None => delegate.debug(message)
    }

    def info(message: => CharSequence, e: Option[Throwable] = None) = if (isInfoEnabled) e match {
      case Some(ex) => delegate.info(message.toString, ex)
      case None => delegate.info(message.toString)
    }

    def warn(message: => CharSequence, e: Option[Throwable] = None) = if (isWarnEnabled) e match {
      case Some(ex) => delegate.warn(message.toString, ex)
      case None => delegate.warn(message.toString)
    }

    def error(message: => CharSequence, e: Option[Throwable] = None) = if (isErrorEnabled) e match {
      case Some(ex) => delegate.error(message.toString, ex)
      case None => delegate.error(message.toString)
    }
  }

}