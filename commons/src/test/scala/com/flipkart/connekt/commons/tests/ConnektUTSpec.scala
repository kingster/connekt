package com.flipkart.connekt.commons.tests

import com.flipkart.connekt.commons.factories.ConnektLogger
import com.flipkart.connekt.commons.services.ConnektConfig
import org.scalatest._

/**
 *
 *
 * @author durga.s
 * @version 11/15/15
 */
abstract class ConnektUTSpec extends FlatSpec
with Matchers with OptionValues with Inside with Inspectors with BeforeAndAfterAll {

  override def beforeAll() = {
    val logConfigFile =  getClass.getClassLoader.getResourceAsStream("logback-test.xml")
    ConnektLogger.init(logConfigFile)

    ConnektConfig(configHost = "config-service.nm.flipkart.com", configPort = 80, configAppVersion = 1)()
  }
}
