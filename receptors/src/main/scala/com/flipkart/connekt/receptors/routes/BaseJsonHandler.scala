package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpHeader, _}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.{PredefinedFromEntityUnmarshallers, Unmarshaller}
import com.flipkart.connekt.commons.iomodels.GenericResponse
import com.flipkart.connekt.receptors.directives.{AsyncDirectives, AuthenticationDirectives, AuthorizationDirectives, HeaderDirectives}
import com.flipkart.connekt.receptors.wire.GenericJsonSupport

import scala.collection.immutable.Seq

/**
 *
 *
 * @author durga.s
 * @version 11/21/15
 */
abstract class BaseJsonHandler extends GenericJsonSupport with Directives with HeaderDirectives with AuthenticationDirectives with AuthorizationDirectives with AsyncDirectives {

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
  
  implicit class RouteUtil(r: GenericResponse) {
    def respondWithHeaders(headers: Seq[HttpHeader]) = responseMarshallable[GenericResponse](r.status, headers, r)
  }

  implicit def GenericResponse2Marshallable(r: GenericResponse): ToResponseMarshallable = responseMarshallable[GenericResponse](r.status, Seq.empty[HttpHeader], r)
}