package com.flipkart.connekt.commons.factories

import com.flipkart.connekt.commons.dao._
import com.flipkart.connekt.commons.helpers.{KafkaConsumer, KafkaProducer}
import com.flipkart.connekt.commons.services._

/**
 *
 *
 * @author durga.s
 * @version 12/8/15
 */
object ServiceFactory {

  var serviceCache = Map[ServiceType.Value, TService]()

  def initMessageService(requestDao: TRequestDao, queueProducerHelper: KafkaProducer, queueConsumerHelper: KafkaConsumer) = {
    serviceCache += ServiceType.MESSAGE -> new IMessageService(requestDao, queueProducerHelper, queueConsumerHelper)
  }

  def initCallbackService(emailCallbackDao: EmailCallbackDao, pnCallbackDao: PNCallbackDao, requestInfoDao: PNRequestDao, emailRequestDao: EmailRequestDao) = {
    serviceCache += ServiceType.CALLBACK ->  new CallbackService(pnCallbackDao,emailCallbackDao, requestInfoDao, emailRequestDao)
  }

  def initAuthorisationService(priv: PrivDao, userInfo: TUserInfo) = {
    serviceCache += ServiceType.AUTHORISATION -> new AuthorisationService(priv,userInfo)
  }

  def getMessageService = serviceCache(ServiceType.MESSAGE).asInstanceOf[TMessageService]

  def getCallbackService = serviceCache(ServiceType.CALLBACK).asInstanceOf[TCallbackService]

  def getAuthorisationService = serviceCache(ServiceType.AUTHORISATION).asInstanceOf[TAuthorisationService]

}

object ServiceType extends Enumeration {
  val MESSAGE, TEMPLATE, CALLBACK, AUTHORISATION = Value
}