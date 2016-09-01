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

object ConnektLogger {

  def init(logConfFilePath: String) = LoggerFactoryConfigurator.configureLog4j2(logConfFilePath)

  def shutdown() = LoggerFactoryConfigurator.shutdownLog4j2()

  def apply(logFile: String) = {
    LogManager.getLogger(logFile)
  }
}

object LogFile {
  final val ACCESS = "ACCESS"
  final val FACTORY = "FACTORY"
  final val SERVICE = "SERVICE"
  final val DAO = "DAO"
  final val WORKERS = "WORKERS"
  final val CLIENTS = "CLIENTS"
  final val PROCESSORS = "PROCESSORS"
  final val CALLBACKS = "CALLBACKS"
}
