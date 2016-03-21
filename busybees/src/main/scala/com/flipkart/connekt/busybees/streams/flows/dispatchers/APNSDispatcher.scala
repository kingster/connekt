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

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.entities.{Channel, MobilePlatform}
import com.flipkart.connekt.commons.factories.{ServiceFactory, ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{APSPayloadEnvelope, PNCallbackEvent, iOSPNPayload}
import com.flipkart.connekt.commons.services.{DeviceDetailsService, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification
import com.relayrides.pushy.apns.{ApnsClient, ClientNotConnectedException}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class APNSDispatcher(appNames: List[String] = List.empty) extends GraphStage[FlowShape[APSPayloadEnvelope, PNCallbackEvent]] {

  type AppName = String
  val in = Inlet[APSPayloadEnvelope]("APNSDispatcher.In")
  val out = Outlet[PNCallbackEvent]("APNSDispatcher.Out")

  override def shape = FlowShape.of(in, out)

  var clients = scala.collection.mutable.Map[AppName, ApnsClient[SimpleApnsPushNotification]]()

  private def handleDispatch(envelope: APSPayloadEnvelope): PNCallbackEvent = {
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
            events += PNCallbackEvent(envelope.messageId, envelope.deviceId, "APNS_ACCEPTED", MobilePlatform.IOS.toString, envelope.appName, "", "", System.currentTimeMillis())
          case false =>
            ConnektLogger(LogFile.PROCESSORS).error("APNSDispatcher:: onPush :: Notification rejected by the APNs gateway: " + pushNotificationResponse.getRejectionReason)

            if (pushNotificationResponse.getTokenInvalidationTimestamp != null) {
              //This device is now invalid remove device registration.
              DeviceDetailsService.get(envelope.appName, envelope.deviceId).map {
                _.filter(_.osName == MobilePlatform.IOS.toString).foreach(d => DeviceDetailsService.delete(envelope.appName, d.deviceId))
              }.get
              events += PNCallbackEvent(envelope.messageId, envelope.deviceId, "APNS_REJECTED_TOKEN_EXPIRED", MobilePlatform.IOS.toString, envelope.appName, "", "", System.currentTimeMillis())
              ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: Token Invalid [${message.token}] since " + pushNotificationResponse.getTokenInvalidationTimestamp)
            } else {
              events += PNCallbackEvent(envelope.messageId, envelope.deviceId, "APNS_REJECTED", MobilePlatform.IOS.toString, envelope.appName, "", "", System.currentTimeMillis())
            }

        }
      } catch {
        case e: ExecutionException =>
          ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: onPush :: Failed to send push notification: ${envelope.messageId}, ${e.getMessage}", e)
          events += PNCallbackEvent(envelope.messageId, envelope.deviceId, "APNS_SEND_FAILURE", MobilePlatform.IOS.toString, envelope.appName, "", "", System.currentTimeMillis())

          if (e.getCause.isInstanceOf[ClientNotConnectedException]) {
            ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher:: onPush :: Waiting for APNSClient to reconnect.")
            client.getReconnectionFuture.await()
            ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher:: onPush :: APNSClient Reconnected.")
          }
      }

    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: onPush :: Failed to send push notification : ${envelope.messageId}, ${e.getMessage}", e)
        events += PNCallbackEvent(envelope.messageId, envelope.deviceId, "APNS_UNKNOWN_FAILURE", MobilePlatform.IOS.toString, envelope.appName, "", "", System.currentTimeMillis())
    }

    events.foreach(e => ServiceFactory.getCallbackService.persistCallbackEvent(e.messageId, s"${e.appName}${e.deviceId}", Channel.PUSH, e))
    events.head
  }

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val envelope = grab(in)
        ConnektLogger(LogFile.PROCESSORS).debug(s"APNSDispatcher:: ON_PUSH for ${envelope.messageId}")
        try {

          val event = handleDispatch(envelope)
          if (isAvailable(out)) {
            push(out, event)
            ConnektLogger(LogFile.PROCESSORS).debug(s"APNSDispatcher:: PUSHED downstream for ${envelope.messageId}")
          }
        } catch {
          case e: Exception =>
            ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: Error ${envelope.messageId}", e)
            if (!hasBeenPulled(in)) {
              ConnektLogger(LogFile.PROCESSORS).debug(s"APNSDispatcher:: PULLED upstream for ${envelope.messageId}")
              pull(in)
            }
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPull")
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).debug(s"APNSDispatcher:: PULLED upstream on downstream pull.")
        }
      }
    })

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

}
