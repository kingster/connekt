package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream._
import com.flipkart.connekt.busybees.models.{GCMRequestTracker, WNSRequestTracker}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor

/**
 * Created by kinshuk.bairagi on 03/03/16.
 */
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
    if(instance.isEmpty) instance = Some(new HttpDispatcher(actorSystemConf))
  }

  def gcmPoolClientFlow = instance.map(_.gcmPoolClientFlow).get

  def wnsPoolClientFlow = instance.map(_.wnsPoolClientFlow).get
}
