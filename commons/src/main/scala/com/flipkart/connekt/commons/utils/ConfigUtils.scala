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
package com.flipkart.connekt.commons.utils

import com.typesafe.config.{ConfigFactory, ConfigException, Config}

object ConfigUtils {

  implicit class configHandyFunctions(val c: Config) {
    def getOrElse[T](key: String, default: T) = try {
      c.getAnyRef(key).asInstanceOf[T]
    } catch {
      case e: ConfigException.Missing => default
    }
  }

  val emptyConf = ConfigFactory.empty()

  implicit class optConfigHelpers(val c: Option[Config]) {
    def getValueOrElse[T](key: String, default: T): T =
      c.getOrElse(emptyConf).getOrElse[T](key, default)
  }

  def getConfEnvironment = getSystemEnv("CONNEKT_ENV").getOrElse("local")

  def getSystemEnv(name:String) = Option(System.getenv(name))

  def getSystemProperty(name:String) = Option(System.getProperty(name))
}
