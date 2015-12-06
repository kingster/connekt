package com.flipkart.connekt.busybees.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.utils.ResponseUtils._
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GCMPayload, PNRequestData}
import com.flipkart.connekt.commons.transmission.HostConnectionHelper._
import com.flipkart.connekt.commons.utils.StringUtils._

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
    ConnektLogger(LogFile.SERVICE).info(s"Fetching deviceDetails: ${pnRequest.appName} ${pnRequest.deviceId} [${pnRequest.getJson}]")
    val deviceDetails = deviceDetailsDao.fetchDeviceDetails(pnRequest.appName, pnRequest.deviceId)
    val gcmRequestPayload = GCMPayload(List[String](deviceDetails.get.token), pnRequest.delayWhileIdle, pnRequest.data.getObj[ObjectNode])
    ConnektLogger(LogFile.SERVICE).info(s"GCM Request payload ${gcmRequestPayload.getJson}")

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
            ConnektLogger(LogFile.SERVICE).info(s"GCM httpRequest ${r.status.isSuccess()} ${t._2}")
            ConnektLogger(LogFile.SERVICE).debug(s"GCM Response :${r.getResponseMessage}")
          case Failure(e) =>
            ConnektLogger(LogFile.SERVICE).error(s"GCM httpRequest failed for ${t._2}, e: ${e.getMessage}")
        }
      case Failure(e) =>
        ConnektLogger(LogFile.SERVICE).error(s"GCM httpRequest future failed for ${pnRequest.deviceId}, e: ${e.getMessage}")
    }
  }
}

object GCMClient {
  lazy val instance = new GCMClient
}
