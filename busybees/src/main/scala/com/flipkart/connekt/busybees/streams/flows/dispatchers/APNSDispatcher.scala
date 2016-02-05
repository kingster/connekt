package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{APSPayload, iOSPNPayload}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.marketing.connekt.BuildInfo
import com.notnoop.apns._
/**
 * Created by kinshuk.bairagi on 05/02/16.
 */
class APNSDispatcher  extends GraphStage[FlowShape[APSPayload, String]] {

  val in = Inlet[APSPayload]("APNSDispatcher.In")
  val out = Outlet[String]("APNSDispatcher.Out")

  override def shape: FlowShape[APSPayload,String] = FlowShape.of(in, out)

  private lazy val localCertPath = BuildInfo.baseDirectory.getParent + "/build/fk-pf-connekt/deploy/usr/local/fk-pf-connekt/certs/apns_cert_retail.p12"

  private lazy val apnsService = APNS.newService()
    .withCert( localCertPath, "flipkart")
    .withProductionDestination()
    .withDelegate(new ApnsDelegate {

    override def messageSent(message: ApnsNotification, resent: Boolean): Unit = {
      println("messageSent" + message)
      println("messageSent resentAttempt" +  resent)
    }

    override def connectionClosed(e: DeliveryError, messageIdentifier: Int): Unit = {
      println(s"connectionClosed $e : $messageIdentifier")
    }

    override def cacheLengthExceeded(newCacheLength: Int): Unit = {
      println(s"cacheLengthExceeded $newCacheLength")

    }

    override def messageSendFailed(message: ApnsNotification, e: Throwable): Unit = {
      println(s"messagefailed : $message")
      e.printStackTrace()

    }

    override def notificationsResent(resendCount: Int): Unit = {
      println(s"notificationsResent : $resendCount")

    }

  })
    .build()

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =  new GraphStageLogic(shape){

    setHandler(in, new InHandler {
      override def onPush(): Unit = try {

        val message = grab(in).asInstanceOf[iOSPNPayload]
        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Received Message: $message")
        val requestId = EnhancedApnsNotification.INCREMENT_ID()

        ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: onPush:: Send Payload: " + message.data.asInstanceOf[AnyRef].getJson)


        val notif = new EnhancedApnsNotification(requestId, EnhancedApnsNotification.MAXIMUM_EXPIRY, message.token, message.data.asInstanceOf[AnyRef].getJson)


        val res = apnsService.push(notif)
        println(res)

        push(out, requestId.toString)
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

      apnsService.start()
      super.preStart()
    }


    override def afterPostStop(): Unit = {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher:: postStop")

      //apnsService.stop()
      super.afterPostStop()
    }

  }
}
