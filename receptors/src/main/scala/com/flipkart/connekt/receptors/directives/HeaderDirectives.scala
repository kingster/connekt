package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
trait HeaderDirectives  {

  def sniffHeaders: Directive1[Seq[HttpHeader]] = BasicDirectives.extract[Seq[HttpHeader]](_.request.headers)

  def sniffXHeaders = BasicDirectives.extract[Seq[HttpHeader]](_.request.headers.filter(_.lowercaseName().startsWith("x-")))

  def getXHeaders = sniffXHeaders.map(httpHeaders =>  httpHeaders.map(h => h.lowercaseName() -> h.value()).toMap.filterKeys(!List("x-api-key").contains(_)))

}
