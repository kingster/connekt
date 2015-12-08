package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.{KafkaConsumerHelper, KafkaProducerHelper}
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.utils.ConfigUtils._
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.Config

import scala.util.control.NonFatal

/**
 *
 *
 * @author durga.s
 * @version 11/26/15
 */
//FIXME: Make this actor based
object PNMessageService extends MessageService {

  var contextConf: Option[Config] = None
  var topicName = "fk-connekt-pn"
  lazy val pnRequestInfoDao = DaoFactory.getRequestInfoDao
  lazy val enqueueHelper = KafkaProducerHelper
  lazy val dequeueHelper = KafkaConsumerHelper

  def init(serviceContextConf: Config) = {
    contextConf = Some(serviceContextConf)
    topicName = contextConf.getValueOrElse[String]("pn.requestQueue", "fk-connekt-pn")
  }

  override def persistRequest(pnRequest: ConnektRequest, isCrucial: Boolean): Option[String] = {
    val connektId = generateId
    try {
      enqueueRequest(pnRequest.copy(id = connektId))
      pnRequestInfoDao.saveRequestInfo(connektId, pnRequest)
      Some(connektId)
    } catch {
      case NonFatal(e) =>
        ConnektLogger(LogFile.DAO).error(s"ConnektRequest persistence failed: ${e.getMessage}", e)
        None
    }
  }

  //FIXME: cleanup, revamp with progressive(not discrete) sla
  protected def enqueueRequest(pnRequest: ConnektRequest) = {
    enqueueHelper.writeMessages(topicName, pnRequest.getJson)
  }

  def dequeueRequest(): Option[ConnektRequest] = {
    dequeueHelper.readMessage(topicName).map(_.getObj[ConnektRequest])
  }

  override def getRequestInfo(connektId: String): Option[ConnektRequest] = {
    try {
      pnRequestInfoDao.fetchRequestInfo(connektId)
    } catch {
      case NonFatal(e) =>
        ConnektLogger(LogFile.DAO).error(s"Fetching ConnektRequestInfo failed for: $connektId", e)
        None
    }
  }
}
