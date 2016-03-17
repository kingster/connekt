package com.flipkart.connekt.commons.tests.factories

import com.flipkart.connekt.commons.tests.ConnektUTSpec
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.xml.XmlConfiguration
import org.apache.logging.log4j.core.config.{ConfigurationSource, Configurator}
import org.slf4j.{Logger, LoggerFactory}

class LoggerFactoryConfiguratorTest extends ConnektUTSpec {

  "LoggerFactory configuration" should "complete without exceptions" in {
    ConnektLogger.init()
  }
  
  "Logging at different levels" should "be re-directed to STDOUT" in {
    implicit val logFile = "connekt.core"
    ConnektLogger(logFile).trace("Sample [trace] message" + System.currentTimeMillis())
    ConnektLogger(logFile).debug("Sample [debug] message" + System.currentTimeMillis())
    ConnektLogger(logFile).info("Sample [info] message" + System.currentTimeMillis())
    ConnektLogger(logFile).warn("Sample [warn] message" + System.currentTimeMillis())
    ConnektLogger(logFile).error("Sample [error] message" + System.currentTimeMillis())
  }

  "LoggerFactory terminate" should "stop without exceptions" in {
    ConnektLogger.stop()
  }
}


object ConnektLogger {

  def init() = {
    System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")

    val context: LoggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val config = new XmlConfiguration(new ConfigurationSource(ClassLoader.getSystemResourceAsStream("log4j2-test.xml")))
    context.start(config)
  }

  def stop() = {
    val context: LoggerContext = LogManager.getContext.asInstanceOf[LoggerContext]
    Configurator.shutdown(context)
  }

  def apply(implicit fileName: String): Logger = LoggerFactory.getLogger(fileName)

}
