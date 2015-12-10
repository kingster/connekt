package com.flipkart.connekt.commons.tests.utils

import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.connekt.commons.utils.UtilsEnv

/**
 *
 *
 * @author durga.s
 * @version 11/15/15
 */
class UtilsEnvTest extends ConnektUTSpec {

  "getConfEnv" should "return [CONNEKT_ENV] system environment variable value as set" in {
    val currentPropValue = System.getProperty("CONNEKT_ENV")
    val updatedPropValue = "local"

    System.setProperty("CONNEKT_ENV", updatedPropValue)
    UtilsEnv.getConfEnv should be (updatedPropValue)
    System.clearProperty("CONNEKT_ENV")

    if(null != currentPropValue)
      System.setProperty("CONNEKT_ENV", currentPropValue)
  }
}
