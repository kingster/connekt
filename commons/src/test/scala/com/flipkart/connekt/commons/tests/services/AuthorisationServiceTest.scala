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
    val result = auth.isAuthorized( "aman.s","write")
    result.isSuccess shouldEqual true
    result.get shouldEqual true
  }

  "AuthorisationService Test " should "add  authorisation" in {
    val auth = ServiceFactory.getAuthorisationService
    auth.addAuthorization("aman.s", UserType.USER, List("r", "e", "f")).isSuccess shouldEqual true
    auth.isAuthorized( "aman.s", "e",).get shouldEqual true
  }

  "AuthorisationService Test " should "remove authorisation " in {
    val auth = ServiceFactory.getAuthorisationService
    auth.removeAuthorization("aman.s", UserType.USER, List("e")).isSuccess shouldEqual true
    auth.isAuthorized( "aman.s", "e",).get shouldEqual false
  }

}
