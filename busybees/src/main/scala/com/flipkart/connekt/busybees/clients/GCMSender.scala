package com.flipkart.connekt.busybees.clients

import akka.actor.Actor
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GCMPayload, GCMProcessed, GCMRejected, GCMSendFailure}
import com.flipkart.connekt.commons.services.CredentialManager
import com.flipkart.connekt.commons.transmission.HostConnectionHelper._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}
/**
 *
 *
 * @author durga.s
 * @version 12/4/15
 */
class GCMSender(host: String, port: Int, api: String, authKey: String) extends Actor {

  def this() = this("android.googleapis.com", 443,"/gcm/send", CredentialManager.getCredential("PN.ConnektSampleApp").password)

  lazy implicit val clientPoolFlow = getPoolClientFlow[String](host, port)
  implicit val contextDispatcher = context.dispatcher

  override def receive: Receive = {
    case (payload: GCMPayload, messageId: String, deviceId: String) =>
      ConnektLogger(LogFile.CLIENTS).debug(s"$messageId GCM requestPayload: $payload")
      val requestEntity = HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), payload.getJson)
      val httpRequest = new HttpRequest(
        HttpMethods.POST,
        api,
        scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", authKey), RawHeader("Content-Type", "application/json")),
        requestEntity
      )

      val fExec = request[String](httpRequest, messageId)
      fExec.onComplete {
        case Success(t) =>
          t._1 match {
            case Success(r) =>
              ConnektLogger(LogFile.CLIENTS).info(s"GCM HttpRequest ${r.status.isSuccess()} ${t._2}")
              val response = Await.result(r.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")),10.seconds)

              sender() ! (if(r.status.isSuccess()) (messageId, deviceId, response.getObj[GCMProcessed]) else (messageId, deviceId, GCMRejected(statusCode = r.status.intValue(), error = response)))
              ConnektLogger(LogFile.CLIENTS).debug(s"GCM Response: ${response}")

            case Failure(e) =>
              sender() ! (messageId, deviceId, GCMSendFailure(e.getMessage))
              ConnektLogger(LogFile.CLIENTS).error(s"GCM httpRequest failed for ${t._2}, e: ${e.getMessage}", e)
          }
        case Failure(e) =>
          sender() ! (messageId, deviceId, GCMSendFailure(e.getMessage))
          ConnektLogger(LogFile.CLIENTS).error(s"GCM httpRequest future failed for $messageId, e: ${e.getMessage}", e)
      }

    case _ =>
      ConnektLogger(LogFile.CLIENTS).error("UnHandled message.")
  }
}
