package com.flipkart.connekt.busybees.processors

import akka.actor.{Actor, Props}
import com.flipkart.connekt.busybees.clients.GCMSender
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ServiceFactory, ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels._

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
      val tokens = pnInfo.deviceId.map(deviceDetailsDao.get(pnInfo.appName, _).get.token)

      val appDataWithId = pnData.data.put("messageId", messageId)
      val gcmPayload = pnInfo.platform.toUpperCase match {
        case "ANDROID" => GCMPNPayload(tokens, pnInfo.delayWhileIdle, appDataWithId)
        case "OPENWEB" => OpenWebGCMPayload(tokens)
      }

      gcmSender ! (gcmPayload, messageId, pnInfo.deviceId.mkString(","))

      ConnektLogger(LogFile.WORKERS).debug(s"GCM Request sent for $messageId")

    case (messageId: String, deviceId: String, p: GCMProcessed) =>
      val eventType = p.results.head.getOrElse("error", "GCM_RECEIVED").toUpperCase
      val event = PNCallbackEvent(messageId = messageId, deviceId = deviceId, platform = "android", eventType = eventType, appName = "", contextId = "", cargo = "", timestamp = System.currentTimeMillis())
      ServiceFactory.getCallbackService.persistCallbackEvent(event.messageId, event.deviceId, Channel.PUSH, event)
      ConnektLogger(LogFile.PROCESSORS).info(s"GCM Response [$messageId], success: ${p.success}, failure: ${p.failure}")

    case (messageId: String, deviceId: String, r: GCMRejected) =>
      val event = PNCallbackEvent(messageId = messageId, deviceId = deviceId, platform = "android", eventType = "GCM_REJECTED", appName = "", contextId = "", cargo = "", timestamp = System.currentTimeMillis())
      ServiceFactory.getCallbackService.persistCallbackEvent(event.messageId, event.deviceId, Channel.PUSH, event)
      ConnektLogger(LogFile.PROCESSORS).info(s"GCM Rejected [$messageId], code: ${r.statusCode}")

    case (messageId: String, deviceId: String, f: GCMSendFailure) =>
      val event = PNCallbackEvent(messageId = messageId, deviceId = deviceId, platform = "android", eventType = "GCM_FAILURE", appName = "", contextId = "", cargo = "", timestamp = System.currentTimeMillis())
      ServiceFactory.getCallbackService.persistCallbackEvent(event.messageId, event.deviceId, Channel.PUSH, event)
      ConnektLogger(LogFile.PROCESSORS).info(s"GCM Send Failure [$messageId], e: ${f.error}")

    case _ =>
      ConnektLogger(LogFile.WORKERS).error(s"Received unknown message type, unable to process.")
  }
}
