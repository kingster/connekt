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
package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.flipkart.connekt.receptors.directives.AuthenticationDirectives
import com.flipkart.connekt.receptors.routes.callbacks.CallbackRoute
import com.flipkart.connekt.receptors.routes.common._
import com.flipkart.connekt.receptors.routes.push.{FetchRoute, RegistrationRoute, SendRoute}
import com.flipkart.connekt.receptors.routes.reports.ReportsRoute
import com.flipkart.connekt.receptors.routes.status.SystemStatus
import com.flipkart.connekt.receptors.routes.stencils.StencilsRoute

class RouteRegistry(implicit mat: ActorMaterializer) extends AuthenticationDirectives {

  val health = new SystemStatus().route
  val ldap = new LdapAuthRoute().route
  val registration = new RegistrationRoute().route
  val send = new SendRoute().route
  val callback = new CallbackRoute().route
  val report = new ReportsRoute().route
  val fetch = new FetchRoute().route
  val stencil = new StencilsRoute().route
  val client = new ClientRoute().route
  val keyChain = new KeyChainRoute().route
  val debugger = new DebuggerRoute().route
  val admin = new AdminRoute().route

  val allRoutes =
    health ~ ldap ~ send ~ registration ~ callback ~ report ~ fetch ~ stencil ~ client ~ keyChain ~ debugger ~ admin
}
