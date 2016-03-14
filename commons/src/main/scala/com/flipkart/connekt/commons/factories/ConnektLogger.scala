package com.flipkart.connekt.commons.factories

import org.apache.logging.log4j.LogManager

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
object ConnektLogger {

  def init(logConfFilePath: String) = LoggerFactoryConfigurator.configureLog4j2(logConfFilePath)

  def shutdown() = LoggerFactoryConfigurator.shutdownLog4j2()

  def apply(logFile: LogFile.Value) = {
    LogManager.getLogger(logFile.toString)
  }
}

object LogFile extends Enumeration {
  type LogFile = Value

  val ACCESS, FACTORY, SERVICE, DAO, WORKERS, CLIENTS, PROCESSORS = Value
}
