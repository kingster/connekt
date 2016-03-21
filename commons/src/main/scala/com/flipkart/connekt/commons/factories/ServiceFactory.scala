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

import com.flipkart.connekt.commons.dao._
import com.flipkart.connekt.commons.helpers.{KafkaConsumerHelper, KafkaProducerHelper}
import com.flipkart.connekt.commons.services.{KeyChainService, _}

object ServiceFactory {

  var serviceCache = Map[ServiceType.Value, TService]()

  def initPNMessageService(requestDao: PNRequestDao, userConfiguration: TUserConfiguration, queueProducerHelper: KafkaProducerHelper, queueConsumerHelper: KafkaConsumerHelper) = {
    serviceCache += ServiceType.PN_MESSAGE -> new MessageService(requestDao, userConfiguration, queueProducerHelper, queueConsumerHelper)
  }

  def initCallbackService(emailCallbackDao: EmailCallbackDao, pnCallbackDao: PNCallbackDao, requestInfoDao: PNRequestDao, emailRequestDao: EmailRequestDao) = {
    serviceCache += ServiceType.CALLBACK -> new CallbackService(pnCallbackDao, emailCallbackDao, requestInfoDao, emailRequestDao)
  }

  def initAuthorisationService(priv: PrivDao, userInfo: TUserInfo) = {
    serviceCache += ServiceType.AUTHORISATION -> new AuthorisationService(priv, userInfo)
    serviceCache += ServiceType.USER_INFO -> new UserInfoService(userInfo)
  }

  def initStorageService(dao: TKeyChainDao) = {
    serviceCache += ServiceType.KEY_CHAIN -> new KeyChainService(dao)
  }

  def getPNMessageService = serviceCache(ServiceType.PN_MESSAGE).asInstanceOf[TMessageService]

  def getCallbackService = serviceCache(ServiceType.CALLBACK).asInstanceOf[TCallbackService]

  def getAuthorisationService = serviceCache(ServiceType.AUTHORISATION).asInstanceOf[TAuthorisationService]

  def getUserInfoService  = serviceCache(ServiceType.USER_INFO).asInstanceOf[UserInfoService]

  def getKeyChainService = serviceCache(ServiceType.KEY_CHAIN).asInstanceOf[TStorageService]

}

object ServiceType extends Enumeration {
  val PN_MESSAGE, TEMPLATE, CALLBACK, USER_INFO, AUTHORISATION, KEY_CHAIN = Value
}
