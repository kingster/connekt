package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.kloud.config.{Bucket, BucketUpdateListener, ConfigClient}
import com.typesafe.config.{ConfigException, Config, ConfigFactory}
import com.flipkart.connekt.commons.behaviors.ConfigAccumulator
import com.flipkart.connekt.commons.utils.UtilsEnv

import scala.collection.mutable
/**
 *
 *
 * @author durga.s
 * @version 11/16/15
 */
class ConnektConfig(configHost: String, configPort: Int, configAppVersion: Int)
  extends ConfigAccumulator {

  val cfgHost = configHost
  val cfgfPort = configPort
  val cfgAppVer = configAppVersion

  var cfgClient: ConfigClient = null

  val bucketNames = mutable.LinkedHashMap[String, String](
    "ROOT_CONF" -> "fk-connekt-root",
    "ENV_OVERRIDE_CONF" -> "fk-connekt-".concat(UtilsEnv.getConfEnv)
  )

  val bucketConfigs = mutable.LinkedHashMap[String, Config]()

  var appConfig: Config = ConfigFactory.empty()

  def readConfigs: List[Config] = {
    cfgClient = new ConfigClient(cfgHost, cfgfPort, cfgAppVer)

    for(bucketName <- bucketNames.values) {
      val bucket = cfgClient.getDynamicBucket(bucketName)
      bucketConfigs.put(bucketName, ConfigFactory.parseMap(bucket.getKeys))

      bucket.addListener(new BucketUpdateListener() {
        override def updated(oldBucket: Bucket, newBucket: Bucket): Unit = {
          ConnektLogger(LogFile.SERVICE).info(s"dynamic bucket $bucketName updated")
          bucketConfigs.put(bucketName, ConfigFactory.parseMap(newBucket.getKeys))

          this.synchronized {
            appConfig = overlayConfigs(bucketConfigs.values.toList: _*)
          }
        }

        override def connected(s: String): Unit = ConnektLogger(LogFile.SERVICE).info(s"dynamic bucket $bucketName connected.")

        override def disconnected(s: String, e: Exception): Unit = ConnektLogger(LogFile.SERVICE).info(s"dynamic bucket $bucketName dis-connected.")

        override def deleted(s: String): Unit = ConnektLogger(LogFile.SERVICE).info(s"dynamic bucket $bucketName deleted.")
      })
    }

    bucketConfigs.values.toList
  }

  def init() = {
    ConnektLogger(LogFile.SERVICE).info("Connekt config init.")
    val configs = readConfigs
    this.synchronized {
      appConfig = overlayConfigs(configs: _*)
    }
  }

  def terminate() = {
    ConnektLogger(LogFile.SERVICE).info("Connekt config client terminating")
    cfgClient.shutdown()
  }
}

object ConnektConfig {
  var instance: ConnektConfig = null

  def apply(configHost: String = "config-service.nm.flipkart.com",
            configPort: Int = 80,
            configAppVersion: Int = 1) = {
    this.synchronized {
      if(null == instance) {
        instance = new ConnektConfig(configHost, configPort, configAppVersion)
        instance.init()
      }
    }
    instance
  }

  def getString(k: String): Option[String] = try {
    Some(instance.appConfig.getString(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getInt(k: String): Option[Int] = try {
    Some(instance.appConfig.getInt(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getDouble(k: String): Option[Double] = try {
    Some(instance.appConfig.getDouble(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getBoolean(k: String): Option[Boolean] = try {
    Some(instance.appConfig.getBoolean(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getConfig(k: String): Option[Config] = try {
    Some(instance.appConfig.getConfig(k))
  } catch {
    case _: ConfigException.Missing => None
  }
}