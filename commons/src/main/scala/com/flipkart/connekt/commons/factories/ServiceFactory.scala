package com.flipkart.connekt.commons.factories

import com.flipkart.connekt.commons.dao.TRequestDao
import com.flipkart.connekt.commons.helpers.{KafkaConsumer, KafkaProducer}
import com.flipkart.connekt.commons.services.{TMessageService, IMessageService, TService}

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

  def getMessageService = serviceCache(ServiceType.MESSAGE).asInstanceOf[TMessageService]
}

object ServiceType extends Enumeration {
  val MESSAGE, TEMPLATE = Value
}