package com.flipkart.connekt.busybees.processors

import akka.actor.{Actor, Props}
import com.flipkart.connekt.busybees.clients.GCMSender
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GCMPayload, PNRequestData, PNRequestInfo}

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
    case (messageId: String, pnInfo: PNRequestInfo, pnData: PNRequestData) =>
      val registrationId = deviceDetailsDao.fetchDeviceDetails(pnInfo.appName, pnInfo.deviceId).get.token
      val appDataWithId = pnData.data.put("messageId", messageId)
      val gcmPayload = GCMPayload(List[String](registrationId), pnInfo.delayWhileIdle, appDataWithId)

      gcmSender ! (gcmPayload, messageId)
      ConnektLogger(LogFile.WORKERS).debug(s"GCM Request sent for $messageId")

    case _ =>
      ConnektLogger(LogFile.WORKERS).error(s"Received unknown message type, unable to process.")
  }
}
