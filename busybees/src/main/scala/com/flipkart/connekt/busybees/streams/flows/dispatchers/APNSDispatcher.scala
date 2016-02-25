package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.util.concurrent.ExecutionException

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.entities.AppleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{APSPayloadEnvelope, APSPayload, iOSPNPayload}
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification
import com.relayrides.pushy.apns.{ApnsClient, ClientNotConnectedException}

/**
 * Created by kinshuk.bairagi on 05/02/16.
 */
class APNSDispatcher extends GraphStage[FlowShape[APSPayloadEnvelope, Either[Throwable, String]]] {

  type AppName = String
  val in = Inlet[APSPayloadEnvelope]("APNSDispatcher.In")
  val out = Outlet[ Either[Throwable, String]]("APNSDispatcher.Out")

  override def shape = FlowShape.of(in, out)

  var clients = scala.collection.mutable.Map[AppName, ApnsClient[SimpleApnsPushNotification]]()

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val envelope = grab(in)
        val message = envelope.apsPayload.asInstanceOf[iOSPNPayload]
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Received Message: $envelope")

        val resultId = StringUtils.generateRandomStr(12)

        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Send Payload: " + message.data.asInstanceOf[AnyRef].getJson)
        val pushNotification = new SimpleApnsPushNotification(message.token, null, message.data.asInstanceOf[AnyRef].getJson)
        val client = clients.getOrElseUpdate(envelope.appName, getAPNSClient(envelope.appName))

        try {
          val pushNotificationResponse = client.sendNotification(pushNotification).get()
          pushNotificationResponse.isAccepted match {
            case true =>
              push(out, Right(resultId))
            case false =>
              ConnektLogger(LogFile.PROCESSORS).error("APNSDispatcher:: onPush :: Notification rejected by the APNs gateway: " + pushNotificationResponse.getRejectionReason)

              if (pushNotificationResponse.getTokenInvalidationTimestamp != null) {
                 //This device is now invalid, triggering cleanup.
                ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: Token Invalid [${message.token}] since " + pushNotificationResponse.getTokenInvalidationTimestamp)
              }

              push(out, Left(new Throwable(pushNotificationResponse.getRejectionReason)))

          }
        } catch {
          case e: ExecutionException =>
            ConnektLogger(LogFile.PROCESSORS).error("APNSDispatcher:: onPush :: Failed to send push notification.", e)

            if (e.getCause.isInstanceOf[ClientNotConnectedException]) {
              ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher:: onPush :: Waiting for APNSClient to reconnect.")
              client.getReconnectionFuture.await()
              ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher:: onPush :: APNSClient Reconnected.")
            }

            if(!hasBeenPulled(in))
              pull(in)
        }
      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: onPush :: ${e.getMessage}", e)
      }

      private def getAPNSClient(appName: String) = {
        val credential = KeyChainManager.getAppleCredentials(appName).get
        val client = new ApnsClient[SimpleApnsPushNotification](credential.getCertificateFile, credential.passkey)
        client.connect(ApnsClient.PRODUCTION_APNS_HOST).await()
        client
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPull")

        pull(in)
      }
    })

    override def preStart(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: preStart")
      /* Create apnsClients for all apps */

      super.preStart()
    }

    override def postStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: postStop")

      // Disconnect all the APNSClients prior to stop
      clients.foreach(kv => {
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: Stopping ${kv._1} APNSClient.")
        kv._2.disconnect()
      })
      super.postStop()
    }
  }
}
