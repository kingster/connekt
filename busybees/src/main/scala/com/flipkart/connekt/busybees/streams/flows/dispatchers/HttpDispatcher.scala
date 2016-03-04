package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.{URLEncoder, URL}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.{HttpHeader, HttpEntity, HttpRequest, HttpResponse}
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.models.{GCMRequestTracker, WNSRequestTracker}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.utils.http.HttpClient
import com.flipkart.utils.http.HttpClient._
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Try}

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
