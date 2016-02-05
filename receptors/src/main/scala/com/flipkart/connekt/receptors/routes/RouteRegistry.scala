package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.flipkart.connekt.receptors.directives.AuthenticationDirectives
import com.flipkart.connekt.receptors.routes.Stencils.StencilsRoute
import com.flipkart.connekt.receptors.routes.callbacks.Callback
import com.flipkart.connekt.receptors.routes.push.{Fetch, LdapAuthentication, Registration, Unicast}
import com.flipkart.connekt.receptors.routes.reports.Reports

/**
 * Created by kinshuk.bairagi on 10/12/15.
 */
class RouteRegistry(implicit mat: ActorMaterializer) extends AuthenticationDirectives {

  def allRoutes = authenticate {
    implicit user => {

      val receptorReqHandler = new Registration().register
      val unicastHandler = new Unicast().unicast
      val callbackHandler = new Callback().callback
      val reportsRoute = new Reports().route
      val fetchRoute = new Fetch().fetch
      val stencilRoute = new StencilsRoute().stencils
      val ldapRoute = new LdapAuthentication().token

      unicastHandler ~ receptorReqHandler ~ callbackHandler ~ reportsRoute ~ fetchRoute ~ stencilRoute ~ ldapRoute
    }
  }

}
