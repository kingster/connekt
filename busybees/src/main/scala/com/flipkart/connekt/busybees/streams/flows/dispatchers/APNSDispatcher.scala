package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.util.concurrent.ExecutionException

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.entities.AppleCredential
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{APSPayload, iOSPNPayload}
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification
import com.relayrides.pushy.apns.{ApnsClient, ClientNotConnectedException}

/**
 * Created by kinshuk.bairagi on 05/02/16.
 */
class APNSDispatcher(credential: AppleCredential) extends GraphStage[FlowShape[APSPayload, Either[Throwable, String]]] {

  val in = Inlet[APSPayload]("APNSDispatcher.In")
  val out = Outlet[ Either[Throwable, String]]("APNSDispatcher.Out")

  override def shape = FlowShape.of(in, out)

  var callback: AsyncCallback[String] = null

  private lazy val apnsClient = new ApnsClient[SimpleApnsPushNotification](credential.getCertificateFile, credential.passkey)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in).asInstanceOf[iOSPNPayload]
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Received Message: $message")

        val resultId = StringUtils.generateRandomStr(12)

        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Send Payload: " + message.data.asInstanceOf[AnyRef].getJson)
        val pushNotification = new SimpleApnsPushNotification(message.token, null, message.data.asInstanceOf[AnyRef].getJson)


        val sendNotificationFuture = apnsClient.sendNotification(pushNotification)

        try {
          val pushNotificationResponse = sendNotificationFuture.get()
          pushNotificationResponse.isAccepted match {
            case true =>
              push(out, Right(resultId))
            case false =>
              ConnektLogger(LogFile.PROCESSORS).error("APNSDispatcher:: onPush :: Notification rejected by the APNs gateway: " + pushNotificationResponse.getRejectionReason)

              if (pushNotificationResponse.getTokenInvalidationTimestamp != null) {
                /**
                 * This device is now invalid, triggering cleanup.
                 */

                ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: Token Invalid [${message.token}] since " + pushNotificationResponse.getTokenInvalidationTimestamp)
              }

              push(out, Left(new Throwable(pushNotificationResponse.getRejectionReason)))

          }
        } catch {
          case e: ExecutionException =>

            ConnektLogger(LogFile.PROCESSORS).error("APNSDispatcher:: onPush :: Failed to send push notification.", e)

            if (e.getCause.isInstanceOf[ClientNotConnectedException]) {
              ConnektLogger(LogFile.PROCESSORS).info("APNSDispatcher:: onPush :: Waiting for client to reconnect…")
              apnsClient.getReconnectionFuture.await()
              ConnektLogger(LogFile.PROCESSORS).info("APNSDispatcher:: onPush :: apnsClient Reconnected.")
            }

            pull(in) //pull back and process the next item
        }


      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher:: onPush :: Error", e)
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

      val connectFuture = apnsClient.connect(ApnsClient.PRODUCTION_APNS_HOST)
      connectFuture.await()

      callback = getAsyncCallback[String] {
        resultId =>
          ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: getAsyncCallback :: " + resultId)
        //push(out, resultId)
      }

      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: preStart callback=" + callback)

      super.preStart()
    }


    override def afterPostStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: postStop")

      //apnsClient.stop()
      super.afterPostStop()
    }

  }
}
