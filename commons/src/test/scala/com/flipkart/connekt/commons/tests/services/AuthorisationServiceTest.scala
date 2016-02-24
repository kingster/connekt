package com.flipkart.connekt.commons.tests.services

import com.flipkart.connekt.commons.entities.UserType
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * @author aman.shrivastava on 12/12/15.
 */
class AuthorisationServiceTest extends CommonsBaseTest {
  "AuthorisationService Test " should "return is Authorized " in {
    val auth = ServiceFactory.getAuthorisationService
    val result = auth.isAuthorized( "aman.s","WRITE")
    result.isSuccess shouldEqual true
    result.get shouldEqual true
  }

  "AuthorisationService Test " should "add  authorisation" in {
    val auth = ServiceFactory.getAuthorisationService
    auth.addAuthorization("aman.s", UserType.USER, List("R", "E", "F")).isSuccess shouldEqual true
    auth.isAuthorized( "aman.s", "F").get shouldEqual true
  }

  "AuthorisationService Test " should "remove authorisation " in {
    val auth = ServiceFactory.getAuthorisationService
    auth.removeAuthorization("aman.s", UserType.USER, List("F")).isSuccess shouldEqual true
    auth.isAuthorized( "aman.s", "F").get shouldEqual false
  }

}
