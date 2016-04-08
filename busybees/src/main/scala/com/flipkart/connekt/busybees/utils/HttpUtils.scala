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
package com.flipkart.connekt.busybees.utils

import akka.http.scaladsl.model.{HttpMessage, HttpResponse}
import akka.stream.Materializer

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object HttpUtils {

  implicit class responseParser(val response: HttpResponse)(implicit fm: Materializer) {
    def getResponseMessage:String = {
      val txtResponse = Await.result(response.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)
      txtResponse
    }
  }
  
  implicit class HttpHeaderUtil(val h: HttpMessage) {
    def optHeader(headerName: String): String = {
      h.headers.find(_.is(headerName)).map(_.value()).orNull
    }
  }
}
