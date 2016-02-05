package com.flipkart.connekt.busybees.utils

import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author durga.s
 * @version 11/29/15
 */
object ResponseUtils {


  implicit class responseParser(val response: HttpResponse)(implicit fm: Materializer) {
    def getResponseMessage:String = {
      val txtResponse = Await.result(response.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)
      txtResponse
    }
  }
}
