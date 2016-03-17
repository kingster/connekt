package com.flipkart.connekt.receptors.tests.routes

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.receptors.routes.push.{SendRoute, RegistrationRoute}
import com.flipkart.connekt.receptors.routes.stencils.StencilsRoute
import org.scalatest.Matchers

import scala.concurrent.duration.FiniteDuration

/**
 * @author aman.shrivastava on 10/12/15.
 */
abstract class BaseRouteTest extends BaseReceptorsTest with Matchers with ScalatestRouteTest {

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration.apply(30, TimeUnit.SECONDS))
  var stencilRoute: server.Route = null
  var registrationRoute: server.Route = null
  var unicastRoute: server.Route = null

  implicit val am = system
  val header = RawHeader("x-api-key", "b0979afd-2ce3-4786-af62-ab53f88204ae")

  override def beforeAll() = {
    super.beforeAll()
    implicit val user = DaoFactory.getUserInfoDao.getUserByKey("r9qA4fF2prQ7qgX0O9NSdFl6A3q50QxB").get
    stencilRoute = new StencilsRoute().stencils
    registrationRoute = new RegistrationRoute().register
    unicastRoute = new SendRoute().route
  }
}
