package com.flipkart.connekt.commons.factories

import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
object ConnektLogger {
  def init(logConfFilePath: String) = LoggerFactoryConfigurator.configure(logConfFilePath)

  def stop() = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop()

  def apply(logFile: LogFile.Value) = Logger(LoggerFactory.getLogger(logFile.toString))
}

object LogFile extends Enumeration {
  type LogFile = Value

  val FACTORY = Value("FACTORY")
  val SERVICE = Value("SERVICE")
  val DAO = Value("DAO")
  val WORKERS = Value("WORKERS")
  val CLIENTS = Value("CLIENTS")
  val PROCESSORS = Value("PROCESSORS")
}
