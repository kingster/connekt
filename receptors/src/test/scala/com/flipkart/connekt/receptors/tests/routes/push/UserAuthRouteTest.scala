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
package com.flipkart.connekt.receptors.tests.routes.push

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.receptors.routes.common.UserAuthRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

class UserAuthRouteTest extends BaseRouteTest {

  val ldapAuthentication = new UserAuthRoute().route
  "LdapAuthentication test" should "return Ok for save " in {

    val payload =
      s"""
         |{
         |	"username": "123",
         |	"password": "123"
         |}
      """.stripMargin

    Post(s"/v1/auth/ldap", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      ldapAuthentication ~>
      check {
        status shouldEqual StatusCodes.Unauthorized
      }

  }

}
