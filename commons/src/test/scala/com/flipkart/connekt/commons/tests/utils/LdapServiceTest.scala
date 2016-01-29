package com.flipkart.connekt.commons.tests.utils

import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.connekt.commons.utils.LdapService

/**
 * Created by avinash.h on 1/28/16.
 */
class LdapServiceTest extends ConnektUTSpec {

  "authenticate" should "return [boolean] ldap authentication valid or not" in {
    LdapService.authenticate("123", "123") should not be true
  }
}
