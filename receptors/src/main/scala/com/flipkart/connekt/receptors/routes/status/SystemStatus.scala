package com.flipkart.connekt.receptors.routes.status

import akka.stream.ActorMaterializer
import com.flipkart.connekt.receptors.routes.BaseJsonHandler

/**
 * Created by kinshuk.bairagi on 10/02/16.
 */
class SystemStatus(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    path("elb-healthcheck") {
      get {
        complete(ELBResponse(0,0, 100))
      }
    }
}

sealed case class ELBResponse(uptime:Long, requests:Long, capacity:Int)