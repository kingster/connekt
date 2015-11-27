package com.flipkart.connekt.commons.helpers

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.typesafe.config.{Config, ConfigException}
import org.apache.commons.pool.impl.GenericObjectPool

/**
 *
 *
 * @author durga.s
 * @version 11/27/15
 */
trait GenericObjectPoolHelper {
  def validatePoolProps(poolName: String, poolProps: Config) = {
    val reqdPoolConf = Map[String, AnyVal](
      "maxActive" -> GenericObjectPool.DEFAULT_MAX_ACTIVE,
      "maxIdle" -> GenericObjectPool.DEFAULT_MAX_IDLE,
      "minEvictableIdleTimeMillis" -> GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
      "timeBetweenEvictionRunsMillis" -> GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
      "whenExhaustedAction" -> GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,
      "enableLifo" -> GenericObjectPool.DEFAULT_LIFO
    )
    
    ConnektLogger(LogFile.FACTORY).info("Verifying requisite configs for %s pool.".format(poolName))
    reqdPoolConf.foreach(kv => {
      try {
        poolProps.getAnyRef(kv._1)
      } catch {
        case e: ConfigException.Missing =>
          ConnektLogger(LogFile.FACTORY).warn("Missing %s default %s shall be applied.".format(kv._1, kv._2.toString))
      }
    })
    ConnektLogger(LogFile.FACTORY).info("%s pool config verification complete.".format(poolName))
  }
}
