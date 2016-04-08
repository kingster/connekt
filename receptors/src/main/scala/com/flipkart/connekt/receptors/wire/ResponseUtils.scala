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
package com.flipkart.connekt.receptors.wire

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.HttpHeader
import com.flipkart.connekt.commons.iomodels.GenericResponse
import com.flipkart.connekt.receptors.wire.GenericJsonSupport._
import scala.collection.immutable.Seq

object ResponseUtils extends JsonToEntityMarshaller {

  private val gm = m.getOrElseUpdate(classOf[GenericResponse], genericMarshaller[GenericResponse]).asInstanceOf[ToEntityMarshaller[GenericResponse]]

  implicit class ResponseUtil(r: GenericResponse) {
    def respondWithHeaders(headers: Seq[HttpHeader]): ToResponseMarshallable = {
      val toResponseMarshaller = PredefinedToResponseMarshallers.fromToEntityMarshaller[GenericResponse](r.status, headers)(gm)
      ToResponseMarshallable(r)(toResponseMarshaller)
    }
  }

  implicit def respond(r: GenericResponse): ToResponseMarshallable = {
    val toResponseMarshaller = PredefinedToResponseMarshallers.fromToEntityMarshaller[GenericResponse](r.status, Seq.empty[HttpHeader])(gm)
    ToResponseMarshallable(r)(toResponseMarshaller)
  }
}
