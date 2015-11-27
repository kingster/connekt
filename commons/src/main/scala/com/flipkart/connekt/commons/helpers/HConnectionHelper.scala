package com.flipkart.connekt.commons.helpers

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.factories.HTableFactoryWrapper
import com.typesafe.config.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseConfiguration, HConstants}

/**
 *
 *
 * @author durga.s
 * @version 11/16/15
 */
object HConnectionHelper {

  def createHbaseConnection(hConnectionConfig: Config): HTableFactory = {

    val hConfig: Configuration = HBaseConfiguration.create()
    hConfig.set(HConstants.ZOOKEEPER_QUORUM, hConnectionConfig.getString(HConstants.ZOOKEEPER_QUORUM))
    hConfig.set(HConstants.ZOOKEEPER_CLIENT_PORT, hConnectionConfig.getString(HConstants.ZOOKEEPER_CLIENT_PORT))
    hConfig.set(HConstants.HBASE_REGIONSERVER_LEASE_PERIOD_KEY, HConstants.DEFAULT_HBASE_REGIONSERVER_LEASE_PERIOD.toString)
    hConfig.set(HConstants.HBASE_RPC_TIMEOUT_KEY, HConstants.DEFAULT_HBASE_RPC_TIMEOUT.toString)

    new HTableFactoryWrapper(hConfig)
  }
}
