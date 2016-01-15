package com.flipkart.connekt.commons.tests.services

import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.connekt.commons.utils.UtilsEnv

/**
 *
 *
 * @author durga.s
 * @version 11/16/15
 */
class ConnektConfigTest extends ConnektUTSpec {

  "ConnektConfig companion apply method" should "return an instance" in {
    val currentAppEnv = UtilsEnv.getConfEnv
    System.setProperty("CONNEKT_ENV", "local")

    val connektConfig = ConnektConfig("config-service.nm.flipkart.com", 80, 1)()
    assert(null != connektConfig)

    val fetchedConfigs = connektConfig.readConfigs
    assert(fetchedConfigs.size > 0)

    if(null != currentAppEnv)
      System.setProperty("CONNEKT_ENV", currentAppEnv)
  }
}
