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
package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.util.Date
import java.util.concurrent.{ExecutionException, TimeUnit}

import akka.stream._
import com.flipkart.connekt.busybees.models.MessageStatus.{InternalStatus, APNSResponseStatus}
import com.flipkart.connekt.busybees.streams.flows.MapGraphStage
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.iomodels.{APSPayloadEnvelope, PNCallbackEvent, iOSPNPayload}
import com.flipkart.connekt.commons.services.{DeviceDetailsService, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification
import com.relayrides.pushy.apns.{ApnsClient, ClientNotConnectedException}

import scala.collection.mutable.ListBuffer

class APNSDispatcher(appNames: List[String] = List.empty) extends MapGraphStage[APSPayloadEnvelope, PNCallbackEvent] {

  private type AppName = String
  var clients = scala.collection.mutable.Map[AppName, ApnsClient[SimpleApnsPushNotification]]()

  override def createLogic(inheritedAttributes: Attributes): SupervisedGraphStageLogic = new SupervisedGraphStageLogic {
    override def preStart(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: preStart")
      /* Create APNSClients for all apps */
      clients ++= appNames.map(app => app -> getAPNSClient(app))
      super.preStart()
    }

    override def postStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: postStop")

      // Disconnect all the APNSClients prior to stop
      clients.foreach(kv => {
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: Stopping ${kv._1} APNSClient.")
        kv._2.disconnect().await(5000)
      })
      super.postStop()
    }
  }

  private def getAPNSClient(appName: String) = {
    ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: Starting $appName APNSClient.")
    val credential = KeyChainManager.getAppleCredentials(appName).get
    val client = new ApnsClient[SimpleApnsPushNotification](credential.getCertificateFile, credential.passkey)
    client.connect(ApnsClient.PRODUCTION_APNS_HOST).await(30, TimeUnit.SECONDS)
    client
  }

  override val map = (envelope: APSPayloadEnvelope) => {
    val events = ListBuffer[PNCallbackEvent]()
    try {

      val message = envelope.apsPayload.asInstanceOf[iOSPNPayload]
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Received Message: $envelope")

      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Send Payload: " + message.data.asInstanceOf[AnyRef].getJson)
      val pushNotification = new SimpleApnsPushNotification(message.token, null, message.data.asInstanceOf[AnyRef].getJson, new Date(message.expiryInMillis))
      val client = clients.getOrElseUpdate(envelope.appName, getAPNSClient(envelope.appName))

      try {

        val pushNotificationResponse = client.sendNotification(pushNotification).get()

        pushNotificationResponse.isAccepted match {
          case true =>
            events += PNCallbackEvent(envelope.messageId, envelope.deviceId, APNSResponseStatus.Received, MobilePlatform.IOS.toString, envelope.appName, "")
          case false =>
            ConnektLogger(LogFile.PROCESSORS).error("APNSDispatcher:: onPush :: Notification rejected by the APNs gateway: " + pushNotificationResponse.getRejectionReason)

            if (pushNotificationResponse.getTokenInvalidationTimestamp != null) {
              //This device is now invalid remove device registration.
              DeviceDetailsService.get(envelope.appName, envelope.deviceId).map {
                _.filter(_.osName == MobilePlatform.IOS.toString).foreach(d => DeviceDetailsService.delete(envelope.appName, d.deviceId))
              }.get
              events += PNCallbackEvent(envelope.messageId, envelope.deviceId, APNSResponseStatus.TokenExpired, MobilePlatform.IOS.toString, envelope.appName, "")
              ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: Token Invalid [${message.token}] since " + pushNotificationResponse.getTokenInvalidationTimestamp)
            } else {
              events += PNCallbackEvent(envelope.messageId, envelope.deviceId, APNSResponseStatus.Rejected, MobilePlatform.IOS.toString, envelope.appName, "")
            }

        }
      } catch {
        case e: ExecutionException =>
          ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: onPush :: Failed to send push notification: ${envelope.messageId}, ${e.getMessage}", e)
          events += PNCallbackEvent(envelope.messageId, envelope.deviceId, InternalStatus.APNSSendError, MobilePlatform.IOS.toString, envelope.appName, "")

          if (e.getCause.isInstanceOf[ClientNotConnectedException]) {
            ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher:: onPush :: Waiting for APNSClient to reconnect.")
            client.getReconnectionFuture.await()
            ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher:: onPush :: APNSClient Reconnected.")
          }
      }

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: onPush :: Failed to send push notification : ${envelope.messageId}, ${e.getMessage}", e)
        events += PNCallbackEvent(envelope.messageId, envelope.deviceId, "APNS_UNKNOWN_FAILURE", MobilePlatform.IOS.toString, envelope.appName, "", "APNSDispatcher::".concat(e.getMessage))
    }

    events.persist
    events.toList
  }
}
