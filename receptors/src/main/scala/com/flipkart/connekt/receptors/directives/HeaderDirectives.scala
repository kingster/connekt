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
package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives

import scala.util.Try

trait HeaderDirectives {

  def sniffHeaders: Directive1[Seq[HttpHeader]] = BasicDirectives.extract[Seq[HttpHeader]](_.request.headers)

  def sniffXHeaders = BasicDirectives.extract[Seq[HttpHeader]](_.request.headers.filter(_.lowercaseName().startsWith("x-")))

  def getXHeaders = sniffXHeaders.map(httpHeaders => httpHeaders.map(h => h.lowercaseName() -> h.value()).toMap.filterKeys(!List("x-api-key").contains(_)))

  def isTestRequest: Directive1[Boolean] = {
    Directives.optionalHeaderValueByName("x-perf-test").map { header ⇒
      header.exists(h => Try(h.trim.toBoolean).getOrElse(false))
    }
  }
}
