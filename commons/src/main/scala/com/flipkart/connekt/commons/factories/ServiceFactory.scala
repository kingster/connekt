package com.flipkart.connekt.commons.factories

import com.flipkart.connekt.commons.dao._
import com.flipkart.connekt.commons.services.{KeyChainService, _}
import com.flipkart.connekt.commons.helpers.{KafkaConsumer, KafkaProducerHelper}
import com.flipkart.connekt.commons.services._

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
object ServiceFactory {

  var serviceCache = Map[ServiceType.Value, TService]()

  def initPNMessageService(requestDao: PNRequestDao, queueProducerHelper: KafkaProducerHelper, queueConsumerHelper: KafkaConsumer) = {
    serviceCache += ServiceType.PN_MESSAGE -> new MessageService(requestDao, queueProducerHelper, queueConsumerHelper)
  }

  def initCallbackService(emailCallbackDao: EmailCallbackDao, pnCallbackDao: PNCallbackDao, requestInfoDao: PNRequestDao, emailRequestDao: EmailRequestDao) = {
    serviceCache += ServiceType.CALLBACK -> new CallbackService(pnCallbackDao, emailCallbackDao, requestInfoDao, emailRequestDao)
  }

  def initAuthorisationService(priv: PrivDao, userInfo: TUserInfo) = {
    serviceCache += ServiceType.AUTHORISATION -> new AuthorisationService(priv, userInfo)
  }

  def initStorageService(dao: TKeyChainDao) = {
    serviceCache += ServiceType.KEY_CHAIN -> new KeyChainService(dao)
  }

  def getPNMessageService = serviceCache(ServiceType.PN_MESSAGE).asInstanceOf[TMessageService]

  def getCallbackService = serviceCache(ServiceType.CALLBACK).asInstanceOf[TCallbackService]

  def getAuthorisationService = serviceCache(ServiceType.AUTHORISATION).asInstanceOf[TAuthorisationService]

  def getKeyChainService = serviceCache(ServiceType.KEY_CHAIN).asInstanceOf[TStorageService]

}

object ServiceType extends Enumeration {
  val PN_MESSAGE, TEMPLATE, CALLBACK, AUTHORISATION, KEY_CHAIN = Value
}