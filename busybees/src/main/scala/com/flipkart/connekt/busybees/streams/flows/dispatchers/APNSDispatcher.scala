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
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher preStart")
      /* Create APNSClients for all apps */
      clients ++= appNames.map(app => app -> getAPNSClient(app))
      super.preStart()
    }

    override def postStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher postStop")

      // Disconnect all the APNSClients prior to stop
      clients.foreach(kv => {
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher stopping ${kv._1} apns-client")
        kv._2.disconnect().await(5000)
      })
      super.postStop()
    }
  }

  private def getAPNSClient(appName: String) = {
    ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher starting $appName apns-client")
    val credential = KeyChainManager.getAppleCredentials(appName).get
    val client = new ApnsClient[SimpleApnsPushNotification](credential.getCertificateFile, credential.passkey)
    client.connect(ApnsClient.PRODUCTION_APNS_HOST).await(30, TimeUnit.SECONDS)
    client
  }

  override val map = (envelope: APSPayloadEnvelope) => {

    val events = ListBuffer[PNCallbackEvent]()

    try {
      val message = envelope.apsPayload.asInstanceOf[iOSPNPayload]
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher received message: ${envelope.messageId}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"APNSDispatcher received message: $envelope")

      val pushNotification = new SimpleApnsPushNotification(message.token, null, message.data.asInstanceOf[AnyRef].getJson, new Date(message.expiryInMillis))
      val client = clients.getOrElseUpdate(envelope.appName, getAPNSClient(envelope.appName))

      try {
        val pushNotificationResponse = client.sendNotification(pushNotification).get()

        pushNotificationResponse.isAccepted match {
          case true =>
            ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher notification accepted by the apns gateway: ${pushNotificationResponse.getRejectionReason}")
            events += PNCallbackEvent(envelope.messageId, envelope.deviceId, APNSResponseStatus.Received, MobilePlatform.IOS.toString, envelope.appName, "")

          case false =>
            ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher notification rejected by the apns gateway: ${pushNotificationResponse.getRejectionReason}")

            if (pushNotificationResponse.getTokenInvalidationTimestamp != null) {
              //This device is now invalid remove device registration.
              DeviceDetailsService.get(envelope.appName, envelope.deviceId).map {
                _.filter(_.osName == MobilePlatform.IOS.toString).foreach(d => DeviceDetailsService.delete(envelope.appName, d.deviceId))
              }.get
              events += PNCallbackEvent(envelope.messageId, envelope.deviceId, APNSResponseStatus.TokenExpired, MobilePlatform.IOS.toString, envelope.appName, "")
              ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher token invalid [${message.token}] since ${pushNotificationResponse.getTokenInvalidationTimestamp}")
            } else {
              events += PNCallbackEvent(envelope.messageId, envelope.deviceId, APNSResponseStatus.Rejected, MobilePlatform.IOS.toString, envelope.appName, "")
            }

        }
      } catch {
        case e: ExecutionException =>
          ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher failed to send push notification: ${envelope.messageId}, ${e.getMessage}", e)
          events += PNCallbackEvent(envelope.messageId, envelope.deviceId, InternalStatus.ProviderSendError, MobilePlatform.IOS.toString, envelope.appName, "")

          if (e.getCause.isInstanceOf[ClientNotConnectedException]) {
            ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher waiting for apns-client to reconnect")
            client.getReconnectionFuture.await()
            ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher apns-client reconnected")
          }
      }

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher failed to send push notification for id: ${envelope.messageId}, ${e.getMessage}", e)
        events += PNCallbackEvent(envelope.messageId, envelope.deviceId, APNSResponseStatus.UnknownFailure, MobilePlatform.IOS.toString, envelope.appName, envelope.contextId, "APNSDispatcher: ".concat(e.getMessage))
    }

    events.persist
    events.toList
  }
}
