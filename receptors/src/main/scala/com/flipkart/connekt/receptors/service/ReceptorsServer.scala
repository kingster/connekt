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
import com.flipkart.connekt.receptors.directives.{AccessLogDirective, CORSDirectives}
import com.flipkart.connekt.receptors.routes.{BaseJsonHandler, RouteRegistry}

import scala.collection.immutable.Seq

object ReceptorsServer extends BaseJsonHandler with AccessLogDirective with CORSDirectives {

  implicit val system = ActorSystem("ckt-receptors")
  implicit val mat = ActorMaterializer.create(system)
  implicit val ec = system.dispatcher

  private val bindHost = ConnektConfig.getString("receptors.bindHost").getOrElse("0.0.0.0")
  private val bindPort = ConnektConfig.getInt("receptors.bindPort").getOrElse(28000)

  var httpService: scala.concurrent.Future[akka.http.scaladsl.Http.ServerBinding] = null

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle{
      case AuthenticationFailedRejection(cause, _ ) =>  logTimedRequestResult {
        complete(responseMarshallable[GenericResponse](
          StatusCodes.Unauthorized, Seq.empty[HttpHeader],
          GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("Authentication Failed, Please Contact connekt-dev@flipkart.com", null)))
        )
      }
      case AuthorizationFailedRejection => logTimedRequestResult {
        complete(responseMarshallable[GenericResponse](
          StatusCodes.Forbidden, Seq.empty[HttpHeader],
          GenericResponse(StatusCodes.Forbidden.intValue, null, Response("UnAuthorised Access, Please Contact connekt-dev@flipkart.com", null)))
        )
      }
      case MalformedRequestContentRejection(msg, _) => logTimedRequestResult {
        complete(responseMarshallable[GenericResponse](
          StatusCodes.BadRequest, Seq.empty[HttpHeader],
          GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Malformed Content, Unable to Process Request", Map("debug" -> msg))))
        )
      }
      case TokenAuthenticationFailedRejection(msg) => logTimedRequestResult {
        complete(responseMarshallable[GenericResponse](
          StatusCodes.Forbidden, Seq.empty[HttpHeader],
          GenericResponse(StatusCodes.Forbidden.intValue, null, Response("OTP Validation Failed.", msg)))
        )
      }
    }.handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete(responseMarshallable[GenericResponse](
        StatusCodes.MethodNotAllowed, Seq.empty[HttpHeader],
        GenericResponse(StatusCodes.MethodNotAllowed.intValue, null, Response(s"Can't do that! Supported: ${names mkString " or "}!", null)))
       )
    }.handleNotFound {
      logTimedRequestResult {
        complete(responseMarshallable[GenericResponse](
          StatusCodes.NotFound, Seq.empty[HttpHeader],
          GenericResponse(StatusCodes.NotFound.intValue, null, Response("Oh man, what you are looking for is long gone.", null)))
        )
      }
    }.result()

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: Throwable =>
      val errorUID: String = UUID.randomUUID.getLeastSignificantBits.abs.toString
      ConnektLogger(LogFile.SERVICE).error(s"API ERROR # -- $errorUID  --  Reason [ ${e.getMessage} ]", e)
      val response = Map("message" -> ("Server Error # " + errorUID), "reason" -> e.getMessage)
      logTimedRequestResult {
        complete(responseMarshallable[GenericResponse](
              StatusCodes.InternalServerError, Seq.empty[HttpHeader],
              GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Internal Server Error", response)))
        )
      }
  }

  val allRoutes = cors {
    new RouteRegistry().allRoutes
  }

  def routeWithLogging = ConnektConfig.getString("http.request.log").getOrElse("true").toBoolean match {
    case true => logTimedRequestResult(allRoutes)
    case false => allRoutes
  }

  def apply() = {
    httpService = Http().bindAndHandle(routeWithLogging, bindHost, bindPort)
  }

  def shutdown() = {
    httpService.flatMap(_.unbind())
      .onComplete(_ => {
        ConnektLogger(LogFile.SERVICE).info("receptor server unbinding complete")
        system.terminate()
      })
  }

}
