package com.flipkart.connekt.busybees.processors

import akka.actor.{Actor, Props}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.clients.GCMSender
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GCMPayload, PNRequestData}
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 12/3/15
 */
class AndroidPNProcessor extends Actor {
  lazy val deviceDetailsDao = DaoFactory.getDeviceDetailsDao
  lazy val gcmSender = context.actorOf(Props[GCMSender])

  override def receive: Receive = {
    case pnData: PNRequestData =>
      val registrationId = deviceDetailsDao.fetchDeviceDetails(pnData.appName, pnData.deviceId).get.token
      val gcmPayload = GCMPayload(List[String](registrationId), pnData.delayWhileIdle, pnData.data.getObj[ObjectNode])

      gcmSender ! (gcmPayload, pnData.requestId)
      ConnektLogger(LogFile.WORKERS).debug(s"GCM Request sent for ${pnData.requestId}")

    case u: Any =>
      ConnektLogger(LogFile.WORKERS).error(s"Received unknown message type, unable to process.")
      ConnektLogger(LogFile.WORKERS).debug(s"m: [${u.toString}]")
  }
}
