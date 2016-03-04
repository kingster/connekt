package com.flipkart.connekt.busybees.streams.flows.dispatchers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream._
import com.flipkart.connekt.busybees.models.{GCMRequestTracker, WNSRequestTracker}

/**
 * Created by kinshuk.bairagi on 03/03/16.
 */
object HttpDispatcher {

  implicit val httpSystem = ActorSystem("http-out")
  implicit val httpMat = ActorMaterializer()
  implicit val ec = httpSystem.dispatcher

  val gcmPoolClientFlow = Http().cachedHostConnectionPoolHttps[GCMRequestTracker]("android.googleapis.com",443)(httpMat)
  val wnsPoolClientFlow = Http().cachedHostConnectionPoolHttps[WNSRequestTracker]("hk2.notify.windows.com")(httpMat)

}
