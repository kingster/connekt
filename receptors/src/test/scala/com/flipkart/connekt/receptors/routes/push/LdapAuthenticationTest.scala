package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.{StatusCodes, MediaTypes, HttpEntity}
import com.flipkart.connekt.receptors.routes.BaseRouteTest

/**
 * Created by avinash.h on 1/21/16.
 */
class LdapAuthenticationTest extends BaseRouteTest {

  val ldapAuthentication = new LdapAuthentication().token
  "LdapAuthentication test" should "return Ok for save " in {

    val payload =
      s"""
         |{
         |	"username": "avinash.h",
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
