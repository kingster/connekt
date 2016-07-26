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

import java.util.UUID

import com.flipkart.connekt.commons.entities.{AppUserConfiguration, Channel, MobilePlatform}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.services.UserConfigurationService
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils._

class UserConfigurationServiceTest extends CommonsBaseTest {

  val clientId = UUID.randomUUID().toString.substring(20)
  val clientQueueName = s"connekt-ut-push-$clientId"
  val appUserConfiguration = new AppUserConfiguration(userId = clientId, channel = Channel.PUSH, queueName = clientQueueName, platforms = "android,ios,windows,openweb", maxRate = 1000)

  "addNewClient in UserConfigurationService" should "add a new client" in {
    UserConfigurationService.add(appUserConfiguration).get
  }

  ignore /*"getTopicNames in MessageService"*/ should "return relevant platform topics" in {
    ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH, Option(MobilePlatform.ANDROID)).get contains clientQueueName shouldEqual true
  }
}
