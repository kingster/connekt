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
package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream._
import com.flipkart.connekt.busybees.models.{GCMRequestTracker, WNSRequestTracker}
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor

class HttpDispatcher(actorSystemConf: Config) {

  implicit val httpSystem: ActorSystem = ActorSystem("http-out", actorSystemConf)
  implicit val httpMat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = httpSystem.dispatcher

  private val gcmPoolClientFlow = Http().cachedHostConnectionPoolHttps[GCMRequestTracker]("android.googleapis.com",443)(httpMat)

  private val wnsPoolClientFlow = Http().cachedHostConnectionPoolHttps[WNSRequestTracker]("hk2.notify.windows.com")(httpMat)
}

object HttpDispatcher {

  private var instance: Option[HttpDispatcher] = None

  def init(actorSystemConf: Config) = {
    if(instance.isEmpty) {
      ConnektLogger(LogFile.SERVICE).info(s"Creating HttpDispatcher actor-system with conf: ${actorSystemConf.toString}")
      instance = Some(new HttpDispatcher(actorSystemConf))
    }
  }

  def gcmPoolClientFlow = instance.map(_.gcmPoolClientFlow).get

  def wnsPoolClientFlow = instance.map(_.wnsPoolClientFlow).get
}
