package com.flipkart.connekt.commons.tests.services

import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.connekt.commons.utils.ConfigUtils
import com.flipkart.connekt.receptors.ReceptorsBoot._

/**
 *
 *
 * @author durga.s
 * @version 11/16/15
 */
class ConnektConfigTest extends ConnektUTSpec {

  "ConnektConfig companion apply method" should "return an instance" in {
    val currentAppEnv = ConfigUtils.getConfEnvironment
    System.setProperty("CONNEKT_ENV", "local")

    val connektConfig = ConnektConfig(configServiceHost, configServicePort)()

    assert(null != connektConfig)

    val fetchedConfigs = connektConfig.bucketConfigs
    assert(fetchedConfigs.size > 0)

    if(null != currentAppEnv)
      System.setProperty("CONNEKT_ENV", currentAppEnv)
  }
}
