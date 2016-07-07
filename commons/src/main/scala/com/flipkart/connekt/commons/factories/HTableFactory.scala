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

import com.flipkart.connekt.commons.connections.TConnectionProvider
import com.typesafe.config.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{BufferedMutator, Connection, Table, TableConfiguration}
import org.apache.hadoop.hbase.{HBaseConfiguration, HConstants, TableName}

import scala.util.Try

class HTableFactory(hConnConfig: Config, connProvider: TConnectionProvider) extends THTableFactory {

  val hConnectionConfig = {
    val hConfig: Configuration = HBaseConfiguration.create()
    hConfig.set(HConstants.ZOOKEEPER_QUORUM, hConnConfig.getString(HConstants.ZOOKEEPER_QUORUM))
    hConfig.set(HConstants.ZOOKEEPER_CLIENT_PORT, hConnConfig.getString(HConstants.ZOOKEEPER_CLIENT_PORT))
    hConfig.set(HConstants.HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD, HConstants.DEFAULT_HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD.toString)
    hConfig.set(HConstants.HBASE_RPC_TIMEOUT_KEY, HConstants.DEFAULT_HBASE_RPC_TIMEOUT.toString)
    hConfig.set(HConstants.ZOOKEEPER_ZNODE_PARENT, hConnConfig.getString(HConstants.ZOOKEEPER_ZNODE_PARENT))
    hConfig.set(TableConfiguration.WRITE_BUFFER_SIZE_KEY, hConnConfig.getString(TableConfiguration.WRITE_BUFFER_SIZE_KEY))
    hConfig.set("hbase.zookeeper.watcher.sync.connected.wait", "5000")
    hConfig
  }

  private var hConnection: Connection = connProvider.createHbaseConnection(hConnectionConfig)

  private def reconnect() = {
    this.synchronized {
      if(null == hConnection || hConnection.isClosed) {
        hConnection = connProvider.createHbaseConnection(hConnectionConfig)
        ConnektLogger(LogFile.FACTORY).warn(s"hbase reconnection successful.")
      } else {
        ConnektLogger(LogFile.FACTORY).warn(s"skipping hbase reconnection, in healthy state.")
      }
    }
  }

  override def shutdown(): Unit = hConnection.close()

  override def getTableInterface(tableName: String): Table = {
    val table = TableName.valueOf(tableName)
    Try(hConnection.getTable(table)).recover {
      case e: IllegalArgumentException =>
        reconnect()
        hConnection.getTable(table)
    }.get
  }

  override def releaseTableInterface(hTableInterface: Table): Unit = hTableInterface.close()

  override def getBufferedMutator(tableName: String): BufferedMutator = {
    val table = TableName.valueOf(tableName)
    Try(hConnection.getBufferedMutator(table)).recover {
      case e: IllegalArgumentException =>
        reconnect()
        hConnection.getBufferedMutator(table)
    }.get
  }

  override def releaseMutator(mutatorInterface: BufferedMutator): Unit = mutatorInterface.close()

  override def getConnection: Connection = hConnection
}
