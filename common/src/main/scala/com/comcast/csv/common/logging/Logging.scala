package com.comcast.csv.common.logging

trait Logging {
  import org.slf4j._

  val loggr = LoggerFactory.getLogger(getClass)

  def trace(message: String) = loggr.trace(message)
  def trace(message: String, error: Throwable) = loggr.trace(message, error)

  def debug(message: String) = loggr.debug(message)
  def debug(message: String, error: Throwable) = loggr.debug(message, error)

  def info(message: String) = loggr.info(message)
  def info(message: String, error: Throwable) = loggr.info(message, error)

  def warn(message: String) = loggr.warn(message)
  def warn(message: String, error: Throwable) = loggr.warn(message, error)

  def error(message: String) = loggr.error(message)
  def error(message: String, error: Throwable) = loggr.error(message, error)
}