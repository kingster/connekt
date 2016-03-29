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
package com.flipkart.connekt.commons.helpers

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.typesafe.config.{Config, ConfigException}
import org.apache.commons.pool.impl.GenericObjectPool

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
    
    ConnektLogger(LogFile.FACTORY).info(s"Verifying requisite configs for $poolName pool.")
    reqdPoolConf.foreach(kv => {
      try {
        poolProps.getAnyRef(kv._1)
      } catch {
        case e: ConfigException.Missing =>
          ConnektLogger(LogFile.FACTORY).warn(s"Missing ${kv._1} default ${kv._2.toString} shall be applied.")
      }
    })
    ConnektLogger(LogFile.FACTORY).info(s"$poolName pool config verification complete.")
  }
}
