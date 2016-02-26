package com.flipkart.connekt.commons.factories

import java.io.{FileInputStream, File, InputStream}

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

  def init(logConf: InputStream) = LoggerFactoryConfigurator.configure(logConf)

  def init(logConfFilePath: String) = {
    val file = new File(logConfFilePath)
    LoggerFactoryConfigurator.configure(new FileInputStream(file))
  }

  def stop() = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop()

  def apply(logFile: LogFile.Value) = Logger(LoggerFactory.getLogger(logFile.toString))
}

object LogFile extends Enumeration {
  type LogFile = Value

  val ACCESS, FACTORY, SERVICE, DAO, WORKERS, CLIENTS, PROCESSORS = Value
}
