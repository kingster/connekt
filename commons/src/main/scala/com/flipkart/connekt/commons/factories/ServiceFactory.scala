package com.flipkart.connekt.commons.factories

import com.flipkart.connekt.commons.dao.{PNCallbackDao, EmailCallbackDao, TRequestDao}
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

  def initCallbackService(emailCallbackDao: EmailCallbackDao, pnCallbackDao: PNCallbackDao) = {
    serviceCache += ServiceType.CALLBACK ->
      (new CallbackService).
        withEmailEventsPersistence(emailCallbackDao).
        withPNEventsPersistence(pnCallbackDao)
  }

  def getMessageService = serviceCache(ServiceType.MESSAGE).asInstanceOf[TMessageService]

  def getCallbackService = serviceCache(ServiceType.CALLBACK).asInstanceOf[TCallbackService]
}

object ServiceType extends Enumeration {
  val MESSAGE, TEMPLATE, CALLBACK = Value
}