package com.flipkart.connekt.receptors.service

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.receptors.directives.AccessLogDirective
import com.flipkart.connekt.receptors.routes.{BaseJsonHandler, RouteRegistry}

import scala.collection.immutable.Seq

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
object ReceptorsServer extends BaseJsonHandler with AccessLogDirective {

  implicit val system = ActorSystem("ckt-receptors")
  implicit val materializer = ActorMaterializer.create(system)
  implicit val ec = system.dispatcher

  private val bindHost = ConnektConfig.getString("receptors.bindHost").getOrElse("0.0.0.0")
  private val bindPort = ConnektConfig.getInt("receptors.bindPort").getOrElse(28000)

  var httpService: scala.concurrent.Future[akka.http.scaladsl.Http.ServerBinding] = null

  def apply() = {

    implicit def rejectionHandler =
      RejectionHandler.newBuilder()
        .handle {
        case AuthorizationFailedRejection =>
          complete(responseMarshallable[GenericResponse](
            StatusCodes.Unauthorized, Seq.empty[HttpHeader],
            GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("UnAuthorised Access, Please Contact connekt-dev@flipkart.com", null)
            )))
        case MalformedRequestContentRejection(msg, _) =>
          complete(responseMarshallable[GenericResponse](
            StatusCodes.BadRequest, Seq.empty[HttpHeader],
            GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Malformed Content, Unable to Process Request", Map("debug" -> msg))
            )))
      }
        .handleAll[MethodRejection] {
        methodRejections =>
          val names = methodRejections.map(_.supported.name)
          complete(responseMarshallable[GenericResponse](
            StatusCodes.MethodNotAllowed, Seq.empty[HttpHeader],
            GenericResponse(StatusCodes.MethodNotAllowed.intValue, null, Response(s"Can't do that! Supported: ${names mkString " or "}!", null)
            )))
      }
        .handleNotFound {
          complete(responseMarshallable[GenericResponse](
          StatusCodes.NotFound, Seq.empty[HttpHeader],
          GenericResponse(StatusCodes.NotFound.intValue, null, Response("Oh man, what you are looking for is long gone.", null)
          )))
        }
        .result()

    implicit def exceptionHandler =
      ExceptionHandler {
        case e: Throwable =>
          val errorUID: String = UUID.randomUUID.getLeastSignificantBits.abs.toString
          ConnektLogger(LogFile.SERVICE).error(s"API ERROR # -- $errorUID  --  Reason [ ${e.getMessage} ]", e)
          val response = Map("message" -> ("Server Error # " + errorUID), "reason" -> e.getMessage)
          complete(responseMarshallable[GenericResponse](
            StatusCodes.InternalServerError, Seq.empty[HttpHeader],
            GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Internal Server Error", response))
          ))
      }

    //TODO : Wrap this route inside CORS
    val allRoutes = new RouteRegistry().allRoutes

    def routeWithLogging = ConnektConfig.getString("http.request.log").getOrElse("true").toBoolean match {
      case true => logTimedRequestResult(allRoutes)
      case false => allRoutes
    }

    httpService = Http().bindAndHandle(routeWithLogging, bindHost, bindPort)
  }


  def shutdown() = {
    httpService.flatMap(_.unbind())
      .onComplete(_ => {
        println("receptor server unbinding complete")
        system.terminate()
      })
  }

}
