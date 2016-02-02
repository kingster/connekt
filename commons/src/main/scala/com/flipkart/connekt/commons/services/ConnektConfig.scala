package com.flipkart.connekt.commons.services

import java.io.IOException
import java.net.UnknownHostException

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.services.ConnektConfig.{ConfigBucketId, BucketName}
import com.flipkart.kloud.config.error.ConfigServiceException
import com.flipkart.kloud.config.{Bucket, BucketUpdateListener, ConfigClient}
import com.typesafe.config.{ConfigException, Config, ConfigFactory}
import com.flipkart.connekt.commons.behaviors.ConfigAccumulator
import com.flipkart.connekt.commons.utils.{NetworkUtils, UtilsEnv}

import scala.collection.mutable
import scala.collection.JavaConversions._
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 11/16/15
 */
class ConnektConfig(configHost: String, configPort: Int, configAppVersion: Int)
                   (bucketIdMap: mutable.LinkedHashMap[BucketName, ConfigBucketId])
  extends ConfigAccumulator {

  val cfgHost = configHost
  val cfgfPort = configPort
  val cfgAppVer = configAppVersion

  var cfgClient: ConfigClient = null

  val bucketConfigs = mutable.LinkedHashMap[String, Config]()

  var appConfig: Config = ConfigFactory.empty()

  def readConfigs(): List[Config] = {
    cfgClient = new ConfigClient(cfgHost, cfgfPort, cfgAppVer)
    ConnektLogger(LogFile.SERVICE).info(s"Buckets to fetch config: [${bucketIdMap.values.toString()}}]")

    for (bucketName <- bucketIdMap.values) {
      val bucket = cfgClient.getDynamicBucket(bucketName)
      bucketConfigs.put(bucketName, ConfigFactory.parseMap(bucket.getKeys))
      ConnektLogger(LogFile.SERVICE).info(s"Fetched config for bucket: $bucketName [$bucket]")

      bucket.addListener(new BucketUpdateListener() {
        override def updated(oldBucket: Bucket, newBucket: Bucket): Unit = {
          ConnektLogger(LogFile.SERVICE).info(s"dynamic bucket $bucketName updated")
          bucketConfigs.put(bucketName, ConfigFactory.parseMap(newBucket.getKeys))

          this.synchronized {
            appConfig = overlayConfigs(bucketConfigs.values.toList: _*)
          }
        }

        override def connected(s: String): Unit = ConnektLogger(LogFile.SERVICE).debug(s"dynamic bucket $bucketName connected.")

        override def disconnected(s: String, e: Exception): Unit = ConnektLogger(LogFile.SERVICE).debug(s"dynamic bucket $bucketName dis-connected.")

        override def deleted(s: String): Unit = ConnektLogger(LogFile.SERVICE).info(s"dynamic bucket $bucketName deleted.")
      })
    }

    bucketConfigs.values.toList
  }

  def init() = {
    ConnektLogger(LogFile.SERVICE).info("Config Init")
    try {
      val configs = readConfigs()
      this.synchronized {
        appConfig = overlayConfigs(configs: _*)
      }
    } catch {
      case uhe@(_: UnknownHostException | _: IOException | _: ConfigServiceException) =>
        if (NetworkUtils.getHostname.contains("local"))
          ConnektLogger(LogFile.SERVICE).warn(s"Offline Mode, Unable to reach $cfgHost")
        else
          throw uhe;
      case e: Throwable =>
        throw e;

    }
    ConnektLogger(LogFile.SERVICE).info("Complete Config: " + ConnektConfig.mask(appConfig.getJson))
  }

  def terminate() = {
    ConnektLogger(LogFile.SERVICE).info("Connekt config client terminating")
    Option(cfgClient).foreach(_.shutdown())
  }
}

object ConnektConfig {

  type BucketName = String
  type ConfigBucketId = String

  var instance: ConnektConfig = null

  def apply(configHost: String = "config-service.nm.flipkart.com", configPort: Int = 80, configAppVersion: Int = 1)
           (bucketIdMap: mutable.LinkedHashMap[BucketName, ConfigBucketId] = mutable.LinkedHashMap("ROOT_CONF" -> "fk-connekt-root", "ENV_OVERRIDE_CONF" -> "fk-connekt-".concat(UtilsEnv.getConfEnv), "CREDENTIALS" -> "fk-connekt-credentials")) = {
    this.synchronized {
      if (null == instance) {
        instance = new ConnektConfig(configHost, configPort, configAppVersion)(bucketIdMap)
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

  def getStringList(k: String): Option[List[String]] = try {
    Some(instance.appConfig.getStringList(k).toList)
  } catch {
    case _: ConfigException.Missing => None
  }


  /**
   * Mask private information
   * @param config
   * @return
   */
  private def mask(config: String): String = {
    config.replaceAll("(\")?password(\")?(\\s)*[:=]+[^\\n]*", "password : * * * * * *")
  }

}