package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.flipkart.connekt.receptors.directives.AuthenticationDirectives
import com.flipkart.connekt.receptors.routes.stencils.StencilsRoute
import com.flipkart.connekt.receptors.routes.callbacks.CallbackRoute
import com.flipkart.connekt.receptors.routes.common.{CredentialsRoute, ClientRoute, LdapAuthRoute}
import com.flipkart.connekt.receptors.routes.push.{FetchRoute, RegistrationRoute, SendRoute}
import com.flipkart.connekt.receptors.routes.reports.ReportsRoute
import com.flipkart.connekt.receptors.routes.status.SystemStatus

/**
 * Created by kinshuk.bairagi on 10/12/15.
 */
class RouteRegistry(implicit mat: ActorMaterializer) extends AuthenticationDirectives {

  val healthReqHandler = new SystemStatus().route
  val ldapRoute = new LdapAuthRoute().route

  def allRoutes = healthReqHandler ~ ldapRoute ~ authenticate {
    implicit user => {
      val receptorReqHandler = new RegistrationRoute().register
      val unicastHandler = new SendRoute().route
      val callbackHandler = new CallbackRoute().callback
      val reportsRoute = new ReportsRoute().route
      val fetchRoute = new FetchRoute().fetch
      val stencilRoute = new StencilsRoute().stencils
      val clientRoute = new ClientRoute().route
      val credentialsRoute = new CredentialsRoute().route

      unicastHandler ~ receptorReqHandler ~ callbackHandler ~ reportsRoute ~ fetchRoute ~ stencilRoute ~ clientRoute ~ credentialsRoute
    }
  }
}
