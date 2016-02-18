package com.flipkart.connekt.receptors.service

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry}
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
object ReceptorsServer extends BaseHandler {

  implicit val system = ActorSystem("ckt-receptors")
  implicit val materializer = ActorMaterializer.create(system)
  implicit val ec = system.dispatcher

  private val bindHost = ConnektConfig.getString("receptors.bindHost").getOrElse("0.0.0.0")
  private val bindPort = ConnektConfig.getInt("receptors.bindPort").getOrElse(28000)

  var httpService: scala.concurrent.Future[akka.http.scaladsl.Http.ServerBinding] = null

  private val logFormat = "%s %s %s %s"

  // logs just the request method and response status at info level
  private def requestMethodAndResponseStatusAsInfo(req: HttpRequest): Any => Option[LogEntry] = {
    case Complete(res) =>
      val remoteIp: String = req.headers.find(_.is("remote-address")).map(_.value()).getOrElse("")
      Some(LogEntry(logFormat.format(remoteIp, req.method.value, req.uri, res.status.intValue()), akka.event.Logging.InfoLevel))
    case _ =>
      None // other kind of responses
  }

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
      case true => DebuggingDirectives.logRequestResult(requestMethodAndResponseStatusAsInfo _)(allRoutes)
      case false =>  allRoutes
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
