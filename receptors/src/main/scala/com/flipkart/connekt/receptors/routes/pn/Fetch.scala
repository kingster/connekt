package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

/**
 *
 *
 * @author durga.s
 * @version 1/14/16
 */
class Fetch extends BaseHandler {

  val maxFetchDuration: Long = 7 * 24 * 3600 * 1000

  val fetch =
    pathPrefix("v1") {
      authenticate { user =>
        path("fetch" / "openwebpn" / Segment / Segment) {
          (appName: String, subscriptionId: String) =>
            authorize(user, "OPENWEBPN_" + appName) {
              get {
                parameters('startTs ?, 'endTs ?){ (startTs, endTs) =>
                  def fetchingPendingOpenWebPNs = ServiceFactory.getMessageService.getFetchRequest(subscriptionId, startTs.getOrElse(System.currentTimeMillis() - maxFetchDuration).asInstanceOf[Long], endTs.getOrElse(System.currentTimeMillis()).asInstanceOf[Long])

                  async(fetchingPendingOpenWebPNs) {
                    case Success(t) => t match {
                      case Success(openWebPNs) =>
                        complete(respond[GenericResponse](
                          StatusCodes.Created, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.OK.intValue, null, Response(s"Pending open-web PNs for $subscriptionId", openWebPNs))
                        ))
                      case Failure(e) =>
                        complete(respond[GenericResponse](
                          StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"Fetching pending open-web PNs for: $subscriptionId failed: ${e.getMessage}", null))
                        ))
                    }
                    case Failure(e) =>
                      complete(respond[GenericResponse](
                        StatusCodes.InternalServerError, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.InternalServerError.intValue, null, Response(s"Fetching pending open-web PNs for: $subscriptionId failed: ${e.getMessage}", null))
                      ))
                  }
                }
              }
            }
        }
      }
    }
}
