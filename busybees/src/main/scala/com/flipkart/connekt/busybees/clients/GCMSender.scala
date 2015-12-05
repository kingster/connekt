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

import scala.util.{Failure, Success}
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
              println("GCM httpRequest %s for %s".format(r.status.isSuccess(), t._2))
              println("GCM response: %s".format(r.getResponseMessage))
              ConnektLogger(LogFile.SERVICE).info("GCM httpRequest %s %s".format(r.status.isSuccess(), t._2))
            case Failure(e) =>
              println("GCM httpRequest failed for %s, e: %s".format(t._2, e.getMessage))
              ConnektLogger(LogFile.SERVICE).error("GCM httpRequest failed for %s, e: %s".format(t._2, e.getMessage))
          }
        case Failure(e) =>
          println("GCM httpRequest future failed for %s, e: %s".format(p._2, e.getMessage))
          ConnektLogger(LogFile.SERVICE).error("GCM httpRequest future failed for %s, e: %s".format(p._2, e.getMessage))
      }
    case u: Any =>
      ConnektLogger(LogFile.CLIENTS).error("UnHandled message: %s".concat(u.toString))
  }
}
