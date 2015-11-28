package com.flipkart.connekt.busybees.providers

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GCMPayload, PNRequestData}
import com.flipkart.connekt.commons.transmission.HostConnectionHelper._
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.busybees.utils.ResponseUtils._
import scala.util.{Failure, Success}

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
class GCMClient {
  lazy val gcmHost = "android.googleapis.com"
  lazy val gcmPort = 443
  lazy val gcmApi = "/gcm/send"
  lazy implicit val poolClientFlow = getPoolClientFlow[String](gcmHost, gcmPort)
  val deviceDetailsDao = DaoFactory.getDeviceDetailsDao

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  def wirePN(pnRequest: PNRequestData, authKey: String) = {
    ConnektLogger(LogFile.SERVICE).info("Fetching deviceDetails: %s %s [%s]".format(pnRequest.appName, pnRequest.deviceId, pnRequest.getJson))
    val deviceDetails = deviceDetailsDao.fetchDeviceDetails(pnRequest.appName, pnRequest.deviceId)
    println("Sending PN to: " + deviceDetails.get.token)
    val gcmRequestPayload = GCMPayload(List[String](deviceDetails.get.token), pnRequest.delayWhileIdle, pnRequest.data.getObj[ObjectNode])
    ConnektLogger(LogFile.SERVICE).info("GCM Request payload %s".format(gcmRequestPayload.getJson))

    val requestEntity = HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`), gcmRequestPayload.getJson)
    val httpRequest = new HttpRequest(
      HttpMethods.POST,
      gcmApi,
      scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", authKey), RawHeader("Content-Type", "application/json")),
      requestEntity
    )

    val fExec = request[String](httpRequest, pnRequest.requestId)
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
        println("GCM httpRequest future failed for %s, e: %s".format(pnRequest.deviceId, e.getMessage))
        ConnektLogger(LogFile.SERVICE).error("GCM httpRequest future failed for %s, e: %s".format(pnRequest.deviceId, e.getMessage))
    }
  }
}

object GCMClient {
  lazy val instance = new GCMClient
}
