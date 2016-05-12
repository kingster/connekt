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
package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import com.flipkart.connekt.receptors.directives._
import com.flipkart.connekt.receptors.wire.JsonToEntityMarshaller

import scala.collection.immutable.Seq

abstract class BaseHandler extends Directives with HeaderDirectives with AuthenticationDirectives with AuthorizationDirectives with MetricsDirectives with AsyncDirectives with PredefinedFromEntityUnmarshallers with PredefinedToEntityMarshallers with JsonToEntityMarshaller {

  /**
   *
   * @param statusCode http response code
   * @param httpHeaders http response headers
   * @param responseObj instance which on serialization forms response payload
   * @param m marshaller converts type `T` to `MessageEntity`
   * @tparam T type of instance to serialize as payload, used in finding suitable `ToEntityMarshaller` in context
   * @return that can be serialized later to `HttpResponse`
   */
  def responseMarshallable[T](statusCode: StatusCode, httpHeaders: Seq[HttpHeader], responseObj: T)
                             (implicit m: ToEntityMarshaller[T]): ToResponseMarshallable = {

    def entity2HttpResponse(obj: MessageEntity): HttpResponse =
      HttpResponse(statusCode, httpHeaders, obj)

    implicit val toHttpResponseMarshaller: ToResponseMarshaller[T] = m.map(entity2HttpResponse)

    ToResponseMarshallable(responseObj)
  }
}
