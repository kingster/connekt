package com.flipkart.connekt.receptors.service

import java.util.UUID

import _root_.akka.actor.ActorSystem
import _root_.akka.http.scaladsl.Http
import _root_.akka.stream.ActorMaterializer
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server.ExceptionHandler
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.receptors.routes.{BaseHandler, RouteRegistry}

import scala.collection.immutable.Seq

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
object ReceptorsServer extends BaseHandler{

  implicit val system = ActorSystem("ckt-receptors")
  implicit val materializer = ActorMaterializer.create(system)
  implicit val ec = system.dispatcher

  private val bindHost = ConnektConfig.getString("receptors.bindHost").getOrElse("0.0.0.0")
  private val bindPort = ConnektConfig.getInt("receptors.bindPort").getOrElse(28000)

  var httpService : scala.concurrent.Future[akka.http.scaladsl.Http.ServerBinding]= null

  def apply() = {

    implicit def exceptionHandler =
      ExceptionHandler {
        case e: Throwable =>
          val errorUID: String = UUID.randomUUID.getLeastSignificantBits.abs.toString
          ConnektLogger(LogFile.SERVICE).error(s"API ERROR # -- ${errorUID}  --  Reason [ ${e.getMessage} ]", e)
          val response = Map("message" -> ("Server Error # " + errorUID), "reason" -> e.getMessage)
          complete(respond[GenericResponse](
            StatusCodes.InternalServerError, Seq.empty[HttpHeader],
            GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Internal Server Error", response))
          ))
      }

    //TODO : Wrap this route inside CORS
    val route = new RouteRegistry().allRoutes

    httpService =  Http().bindAndHandle(route, bindHost, bindPort)
  }


  def shutdown() = {
    httpService.flatMap(_.unbind())
      .onComplete(_ => {
      println("receptor server unbinding complete")
      system.terminate()
    })
  }

}
