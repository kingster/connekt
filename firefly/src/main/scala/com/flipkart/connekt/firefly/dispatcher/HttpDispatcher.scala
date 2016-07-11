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
package com.flipkart.connekt.firefly.dispatcher

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.flipkart.connekt.firefly.sinks.http.HttpRequestTracker
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor

class HttpDispatcher(actorSystemConf: Config) {

  implicit val httpSystem: ActorSystem = ActorSystem("firefly-http-out", actorSystemConf)
  implicit val httpMat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = httpSystem.dispatcher

  val callbackHttpPoolFlow = Http().superPool[HttpRequestTracker]()(httpMat)
}

object HttpDispatcher {

  var dispatcher: Option[HttpDispatcher] = None

  def apply(config: Config) = {
    if(dispatcher.isEmpty) {
      dispatcher = Some(new HttpDispatcher(config))
    }
  }

  def httpFlow = dispatcher.map(_.callbackHttpPoolFlow).get

}
