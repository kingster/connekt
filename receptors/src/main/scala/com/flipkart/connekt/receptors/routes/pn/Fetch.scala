package com.flipkart.connekt.receptors.routes.pn

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq

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
                  def fetchingPendingOpenWebPNs = /* FIX_ME */ List[ConnektRequest]()
                  complete(respond[GenericResponse](
                    StatusCodes.Created, Seq.empty[HttpHeader],
                    GenericResponse(StatusCodes.OK.intValue, null, Response(s"Pending open-web PNs for $subscriptionId", List[ConnektRequest]()))
                  ))
                }
              }
            }
        }
      }
    }
}
