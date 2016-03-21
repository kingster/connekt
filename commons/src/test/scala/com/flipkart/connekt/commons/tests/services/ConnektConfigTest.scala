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
package com.flipkart.connekt.commons.tests.services

import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.connekt.commons.utils.ConfigUtils

class ConnektConfigTest extends ConnektUTSpec {

  "ConnektConfig companion apply method" should "return an instance" in {
    val currentAppEnv = ConfigUtils.getConfEnvironment
    System.setProperty("CONNEKT_ENV", "local")

    val connektConfig = ConnektConfig(configServiceHost, configServicePort)(Seq("fk-connekt-root", "fk-connekt-".concat(ConfigUtils.getConfEnvironment), "fk-connekt-receptors", "fk-connekt-busybees", "fk-connekt-busybees-akka"))

    assert(null != connektConfig)

    val fetchedConfigs = connektConfig.bucketConfigs
    assert(fetchedConfigs.size > 0)

    if(null != currentAppEnv)
      System.setProperty("CONNEKT_ENV", currentAppEnv)
  }
}
