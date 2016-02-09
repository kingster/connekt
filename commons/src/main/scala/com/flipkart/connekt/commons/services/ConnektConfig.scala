package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.utils.UtilsEnv
import com.flipkart.utils.config.KloudConfig
import com.typesafe.config.Config


object ConnektConfig {

  var instance: KloudConfig = null

  def apply(configHost: String = "10.47.0.101", configPort: Int = 80)
           (bucketIdMap: Seq[ String] = Seq( "fk-connekt-root", "fk-connekt-".concat(UtilsEnv.getConfEnv))) = {
    this.synchronized {
      if (null == instance) {
        instance = new KloudConfig(configHost, configPort)(bucketIdMap)
        instance.init()
      }
    }
    instance
  }

  def getString(k: String): Option[String] = instance.getString(k)

  def getInt(k: String): Option[Int] = instance.getInt(k)

  def getDouble(k: String): Option[Double] = instance.getDouble(k)

  def getBoolean(k: String): Option[Boolean] = instance.getBoolean(k)

  def getConfig(k: String): Option[Config] = instance.getConfig(k)

}