/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.services

import com.flipkart.concord.config.TConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._


object ConnektConfig {

  lazy val logger = LoggerFactory.getLogger(getClass)
  var instance: TConfig = null

  def apply(configHost: String, configPort: Int, apiVersion: Int)(bucketIds: Seq[String])(configFile: String = null) = {
    this.synchronized {
      if (null == instance) {
        try {
          val configLoaderClass = Class.forName("com.flipkart.connekt.util.config.KloudConfig")
          instance = configLoaderClass.getConstructor(classOf[Seq[String]], classOf[Option[String]], classOf[Option[Int]], classOf[Option[Int]]).newInstance(bucketIds, Option(configHost), Option(configPort), Option(apiVersion)).asInstanceOf[TConfig]
          configLoaderClass.getMethod("init").invoke(instance)
        } catch {
          case e: Exception =>
            logger.error("Failed to init KloudConfig, fallback to user-defined conf file usage.", e)
            val config = ConfigFactory.parseResources(configFile)
            instance.applyConfig(config)
        }
      }
    }
    instance
  }

  def getList[V](k: String): List[V] = {
    instance.get[V](k).map(_.asInstanceOf[java.util.ArrayList[V]].asScala.toList).getOrElse(Nil)
  }

  def getOrElse[V](k: String, default: V): V = instance.getOrElse(k, default)

  def get[V](k: String): Option[V] = instance.get[V](k)

  def getString(k: String): Option[String] = instance.getString(k)

  def getInt(k: String): Option[Int] = instance.get[Int](k)

  def getDouble(k: String): Option[Double] = instance.get[Double](k)

  def getBoolean(k: String): Option[Boolean] = instance.get[Boolean](k)

  def getConfig(k: String): Option[Config] = instance.getConfig(k)

}
