package com.flipkart.connekt.commons.factories

import java.io.{InputStream, File}

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.LoggerFactory

/**
 *
 *
 * @author durga.s
 * @version 11/19/15
 */
object LoggerFactoryConfigurator {

  @throws[Exception]
  def configure(stream: InputStream) = {
    val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val configurator = new JoranConfigurator()
    configurator.setContext(context)

    configurator.doConfigure(stream)
    StatusPrinter.printInCaseOfErrorsOrWarnings(context)
  }
}
