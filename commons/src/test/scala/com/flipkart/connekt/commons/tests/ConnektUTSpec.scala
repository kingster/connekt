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
package com.flipkart.connekt.commons.tests

import com.flipkart.connekt.commons.core.BaseApp
import com.flipkart.connekt.commons.factories.ConnektLogger
import com.flipkart.connekt.commons.utils.ConfigUtils
import org.scalatest._

abstract class ConnektUTSpec extends FlatSpec  with Matchers with OptionValues with Inside with Inspectors with BeforeAndAfterAll with BaseApp {

  override def beforeAll() = {

    val logConfigFile = ConfigUtils.getSystemProperty("log4j.configurationFile").getOrElse("log4j2-test.xml")
    ConnektLogger.init(logConfigFile)

  }
}
