package com.flipkart.connekt.commons.tests

import com.flipkart.connekt.commons.core.BaseApp
import com.flipkart.connekt.commons.factories.ConnektLogger
import org.scalatest._

/**
 *
 *
 * @author durga.s
 * @version 11/15/15
 */
abstract class ConnektUTSpec extends FlatSpec  with Matchers with OptionValues with Inside with Inspectors with BeforeAndAfterAll with BaseApp {

  override def beforeAll() = {

    val logConfigFile = System.getProperty("user.dir").concat("/commons/src/main/resources/log4j2-test.xml")
    ConnektLogger.init(logConfigFile)

  }
}
