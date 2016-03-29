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
package com.flipkart.connekt.busybees.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import com.flipkart.connekt.busybees.utils.ResponseUtils._
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GCMPNPayload, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.transmission.HostConnectionHelper._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.{Failure, Success}

class GCMClient {
  lazy val gcmHost = "android.googleapis.com"
  lazy val gcmPort = 443
  lazy val gcmApi = "/gcm/send"
  lazy implicit val poolClientFlow = getPoolClientFlow[String](gcmHost, gcmPort)
  val deviceDetailsDao = DaoFactory.getDeviceDetailsDao

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  def wirePN(requestId: String, pnRequestInfo: PNRequestInfo, pnRequestData: PNRequestData, authKey: String) = {
    ConnektLogger(LogFile.SERVICE).info(s"Fetching deviceDetails: ${pnRequestInfo.appName} ${pnRequestInfo.deviceIds} [${pnRequestInfo.getJson}]")
    val tokens = pnRequestInfo.deviceIds.map(deviceDetailsDao.get(pnRequestInfo.appName, _).get.token)

    val gcmRequestPayload = GCMPNPayload(tokens,Option( pnRequestInfo.delayWhileIdle), pnRequestData.data)
    ConnektLogger(LogFile.SERVICE).info(s"GCM Request payload ${gcmRequestPayload.getJson}")

    val requestEntity = HttpEntity(ContentTypes.`application/json`, gcmRequestPayload.getJson.getBytes("UTF-8"))
    val httpRequest = new HttpRequest(
      HttpMethods.POST,
      gcmApi,
      scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", authKey), RawHeader("Content-Type", "application/json")),
      requestEntity
    )

    val fExec = request[String](httpRequest, requestId)
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
        ConnektLogger(LogFile.SERVICE).error(s"GCM httpRequest future failed for ${pnRequestInfo.deviceIds}, e: ${e.getMessage}")
    }
  }
}

object GCMClient {
  lazy val instance = new GCMClient
}
