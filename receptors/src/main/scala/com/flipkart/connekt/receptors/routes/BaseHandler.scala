package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpHeader, _}
import akka.http.scaladsl.server.Directives
import com.flipkart.connekt.receptors.directives.{AuthorizationDirectives, AsyncDirectives, AuthenticationDirectives, HeaderDirectives}
import com.flipkart.connekt.receptors.wire.GenericJsonSupport

import scala.collection.immutable.Seq

/**
 *
 *
 * @author durga.s
 * @version 11/21/15
 */
abstract class BaseHandler extends GenericJsonSupport with Directives with HeaderDirectives with AuthenticationDirectives with AuthorizationDirectives with AsyncDirectives {

  /**
   *
   * @param statusCode http response code
   * @param httpHeaders http response headers
   * @param responseObj instance which on serialization forms response payload
   * @param m marshaller converts type `T` to `MessageEntity`
   * @tparam T type of instance to serialize as payload, used in finding suitable `ToEntityMarshaller` in context
   * @return that can be serialized later to `HttpResponse`
   */
  def respond[T](statusCode: StatusCode, httpHeaders: Seq[HttpHeader], responseObj: T)
                (implicit m: ToEntityMarshaller[T]): ToResponseMarshallable = {

    def entity2HttpResponse(obj: MessageEntity): HttpResponse =
      HttpResponse(statusCode, httpHeaders, obj)

    implicit val toHttpResponseMarshaller: ToResponseMarshaller[T] = m.map(entity2HttpResponse)

    ToResponseMarshallable(responseObj)
  }
}
