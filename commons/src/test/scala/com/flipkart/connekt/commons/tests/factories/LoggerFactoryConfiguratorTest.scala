package com.flipkart.connekt.commons.tests.factories

import java.io.{FileInputStream, File}
import ch.qos.logback.classic.LoggerContext
import com.flipkart.connekt.commons.factories.LoggerFactoryConfigurator
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import org.slf4j.{Logger, LoggerFactory}

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
class LoggerFactoryConfiguratorTest extends ConnektUTSpec {

  "LoggerFactory configuration" should "complete without exceptions" in {
    ConnektLogger.init()
  }
  
  "Logging at different levels" should "be re-directed to STDOUT" in {
    implicit val logFile = "connekt.core"
    ConnektLogger(logFile).trace("Sample [trace] message" + System.currentTimeMillis())
    ConnektLogger(logFile).debug("Sample [debug] message" + System.currentTimeMillis())
    ConnektLogger(logFile).info("Sample [info] message" + System.currentTimeMillis())
    ConnektLogger(logFile).warn("Sample [warn] message" + System.currentTimeMillis())
    ConnektLogger(logFile).error("Sample [error] message" + System.currentTimeMillis())
  }

  "LoggerFactory terminate" should "stop without exceptions" in {
    ConnektLogger.stop()
  }
}


object ConnektLogger {

  def init() = {
    val confFilePath = System.getProperty("user.dir").concat("/commons/src/test/resources/logback-test.xml")
    val file = new File(confFilePath)
    LoggerFactoryConfigurator.configure(new FileInputStream(file))
  }

  def stop() = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop()

  def apply(implicit fileName: String): Logger = LoggerFactory.getLogger(fileName)

}
