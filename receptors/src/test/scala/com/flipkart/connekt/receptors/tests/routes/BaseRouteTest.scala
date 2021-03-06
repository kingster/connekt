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
package com.flipkart.connekt.receptors.tests.routes

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.receptors.routes.common.ClientRoute
import com.flipkart.connekt.receptors.routes.push.{RegistrationRoute, SendRoute}
import com.flipkart.connekt.receptors.routes.stencils.StencilsRoute
import org.scalatest.Matchers

import scala.concurrent.duration.FiniteDuration

abstract class BaseRouteTest extends BaseReceptorsTest with Matchers with ScalatestRouteTest {

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration.apply(30, TimeUnit.SECONDS))
  var stencilRoute: server.Route = null
  var registrationRoute: server.Route = null
  var sendRoute: server.Route = null
  var clientRoute: server.Route = null

  implicit val am = system
  val header = RawHeader("x-api-key", "sandbox-key-01")
  implicit var user: AppUser = null

  override def beforeAll() = {
    super.beforeAll()
    user = new AppUser("test", "test", "", "")
    stencilRoute = new StencilsRoute().route
    registrationRoute = new RegistrationRoute().route
    sendRoute = new SendRoute().route
    clientRoute = new ClientRoute().route
  }
}
