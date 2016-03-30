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
package com.flipkart.connekt.commons.core

import java.util.Properties

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

trait BaseApp {

  lazy val (configServiceHost, configServicePort): (String, Int) = {
    try {
      val prop = new Properties()
      prop.load(getClass.getClassLoader.getResourceAsStream("config.properties"))
      (prop.getProperty("config.host"), new Integer(prop.getProperty("config.port")))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("Config.Prop Load Failed", e)
        sys.exit(1)
    }
  }

}
