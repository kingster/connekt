package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.flipkart.connekt.receptors.directives.AuthenticationDirectives
import com.flipkart.connekt.receptors.routes.Stencils.StencilsRoute
import com.flipkart.connekt.receptors.routes.callbacks.Callback
import com.flipkart.connekt.receptors.routes.push.{Fetch, LdapAuthentication, Registration, Unicast}
import com.flipkart.connekt.receptors.routes.reports.Reports
import com.flipkart.connekt.receptors.routes.status.SystemStatus

/**
 * Created by kinshuk.bairagi on 10/12/15.
 */
class RouteRegistry(implicit mat: ActorMaterializer) extends AuthenticationDirectives {

  val healthReqHandler = new SystemStatus().route
  val ldapRoute = new LdapAuthentication().token

  def allRoutes = healthReqHandler ~ ldapRoute ~ authenticate {
    implicit user => {
      val receptorReqHandler = new Registration().register
      val unicastHandler = new Unicast().unicast
      val callbackHandler = new Callback().callback
      val reportsRoute = new Reports().route
      val fetchRoute = new Fetch().fetch
      val stencilRoute = new StencilsRoute().stencils

      unicastHandler ~ receptorReqHandler ~ callbackHandler ~ reportsRoute ~ fetchRoute ~ stencilRoute
    }
  }
}
