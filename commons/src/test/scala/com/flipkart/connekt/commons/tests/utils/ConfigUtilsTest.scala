package com.flipkart.connekt.commons.tests.utils

import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.connekt.commons.utils.ConfigUtils

/**
 *
 *
 * @author durga.s
 * @version 11/15/15
 */
class ConfigUtilsTest extends ConnektUTSpec {

  "getConfEnv" should "return [CONNEKT_ENV] system environment variable value as set" in {
    ConfigUtils.getConfEnvironment should not be null
  }
}
