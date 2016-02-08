package com.flipkart.connekt.commons.core

import java.util.Properties

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}

/**
 * Created by kinshuk.bairagi on 08/02/16.
 */
trait BaseApp {

  lazy val (configServiceHost, configServicePort): (String, Int) = {
    try {
      val prop = new Properties()
      prop.load(getClass.getClassLoader.getResourceAsStream("config.properties"))
      (prop.getProperty("config.host"), new Integer(prop.getProperty("config.port")))
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("Config.Prop Load Failed", e)
        sys.exit(1)
    }
  }

}
