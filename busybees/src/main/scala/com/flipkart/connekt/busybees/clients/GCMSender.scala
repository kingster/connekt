package com.flipkart.connekt.busybees.clients

import akka.actor.Actor
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.busybees.utils.ResponseUtils._
import com.flipkart.connekt.commons.entities.Credentials
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.GCMPayload
import com.flipkart.connekt.commons.transmission.HostConnectionHelper._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.util.{Failure, Success}
import scala.concurrent.duration._
/**
 *
 *
 * @author durga.s
 * @version 12/4/15
 */
class GCMSender(host: String, port: Int, api: String, authKey: String) extends Actor {

  def this() = this("android.googleapis.com", 443,"/gcm/send", Credentials.sampleAppCred)

  lazy implicit val clientPoolFlow = getPoolClientFlow[String](host, port)
  implicit val contextDispatcher = context.dispatcher

  override def receive: Receive = {
    case p: (GCMPayload, String) =>
      ConnektLogger(LogFile.CLIENTS).debug(s"${p._2} GCM requestPayload: ${p._1}")
      val requestEntity = HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), p._1.getJson)
      val httpRequest = new HttpRequest(
        HttpMethods.POST,
        api,
        scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", authKey), RawHeader("Content-Type", "application/json")),
        requestEntity
      )

      val fExec = request[String](httpRequest, p._2)
      fExec.onComplete {
        case Success(t) =>
          t._1 match {
            case Success(r) =>
              ConnektLogger(LogFile.CLIENTS).info(s"GCM HttpRequest ${r.status.isSuccess()} ${t._2}")
              val response = Await.result(r.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")),10.seconds)
              ConnektLogger(LogFile.CLIENTS).debug(s"GCM Response: ${response}")
              //TODO : handlers

            case Failure(e) =>
              ConnektLogger(LogFile.CLIENTS).error(s"GCM httpRequest failed for ${t._2}, e: ${e.getMessage}", e)
          }
        case Failure(e) =>
          ConnektLogger(LogFile.CLIENTS).error(s"GCM httpRequest future failed for ${p._2}, e: ${e.getMessage}", e)
      }
    case _ =>
      ConnektLogger(LogFile.CLIENTS).error("UnHandled message.")
  }
}
