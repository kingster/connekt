package com.flipkart.connekt.commons.factories

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.xml.XmlConfiguration
import org.apache.logging.log4j.core.config.{ConfigurationSource, Configurator}

/**
 *
 *
 * @author durga.s
 * @version 11/19/15
 */
object LoggerFactoryConfigurator {

  def configureLog4j2(log4j2ConfigFile: String, async: Boolean = true) = {
    if(async)
      System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")

    val context: LoggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val configStream = getClass.getClassLoader.getResourceAsStream(log4j2ConfigFile)
    val config = new XmlConfiguration(new ConfigurationSource(configStream))
    context.start(config)
  }

  def shutdownLog4j2() = {
    val context: LoggerContext = LogManager.getContext.asInstanceOf[LoggerContext]
    Configurator.shutdown(context)
  }
}
