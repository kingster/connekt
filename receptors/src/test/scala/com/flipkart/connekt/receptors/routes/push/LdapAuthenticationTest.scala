package com.flipkart.connekt.receptors.routes.push

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.receptors.routes.BaseRouteTest
import com.flipkart.connekt.receptors.routes.common.LdapAuthentication

/**
 * Created by avinash.h on 1/21/16.
 */
class LdapAuthenticationTest extends BaseRouteTest {

  val ldapAuthentication = new LdapAuthentication().route
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
