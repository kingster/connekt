package com.flipkart.connekt.receptors.routes.status

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq

/**
 * Created by kinshuk.bairagi on 10/02/16.
 */
class SystemStatus(implicit am: ActorMaterializer) extends BaseHandler  {

  val route =
    path("elb-healthcheck") {
      get {
        complete(respond[ELBResponse](
          StatusCodes.OK, Seq.empty[HttpHeader],
          ELBResponse(0,0, 100)
        ))
      }
    }
}

sealed case class ELBResponse(uptime:Long, requests:Long, capacity:Int)