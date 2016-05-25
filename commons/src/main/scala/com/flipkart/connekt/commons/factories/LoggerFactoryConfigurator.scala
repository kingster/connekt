/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.factories

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.xml.XmlConfiguration
import org.apache.logging.log4j.core.config.{ConfigurationSource, Configurator}

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
    val context: LoggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
    Configurator.shutdown(context)
  }
}
