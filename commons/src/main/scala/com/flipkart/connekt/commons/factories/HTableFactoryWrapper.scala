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
package com.flipkart.connekt.commons.factories

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.connections.TConnectionProvider
import com.typesafe.config.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{BufferedMutator, Connection, Table}
import org.apache.hadoop.hbase.{HBaseConfiguration, HConstants, TableName}

class HTableFactoryWrapper(hConnConfig: Config, connProvider: TConnectionProvider) extends HTableFactory {

  val hConnectionConfig = {
    val hConfig: Configuration = HBaseConfiguration.create()
    hConfig.set(HConstants.ZOOKEEPER_QUORUM, hConnConfig.getString(HConstants.ZOOKEEPER_QUORUM))
    hConfig.set(HConstants.ZOOKEEPER_CLIENT_PORT, hConnConfig.getString(HConstants.ZOOKEEPER_CLIENT_PORT))
    hConfig.set(HConstants. HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD, HConstants.DEFAULT_HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD.toString)
    hConfig.set(HConstants.HBASE_RPC_TIMEOUT_KEY, HConstants.DEFAULT_HBASE_RPC_TIMEOUT.toString)
    hConfig.set(HConstants.ZOOKEEPER_ZNODE_PARENT, hConnConfig.getString(HConstants.ZOOKEEPER_ZNODE_PARENT))
    hConfig.set("hbase.zookeeper.watcher.sync.connected.wait", "5000")
    hConfig
  }

  var hConnection: Connection = connProvider.createHbaseConnection(hConnectionConfig)

  override def shutdown(): Unit = hConnection.close()

  override def getTableInterface(tableName: String): Table = hConnection.getTable(TableName.valueOf(tableName))

  override def releaseTableInterface(hTableInterface: Table): Unit = hTableInterface.close()

  override def getBufferedMutator(tableName: String): BufferedMutator = hConnection.getBufferedMutator(TableName.valueOf(tableName))

  override def releaseMutator(mutatorInterface: BufferedMutator): Unit = mutatorInterface.close()
}
