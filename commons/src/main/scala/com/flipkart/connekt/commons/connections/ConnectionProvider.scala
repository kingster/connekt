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
package com.flipkart.connekt.commons.connections

import java.util.Properties
import javax.sql.DataSource

import com.couchbase.client.java.{Cluster, CouchbaseCluster}
import org.apache.commons.dbcp2.BasicDataSourceFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}

import scala.collection.JavaConverters._

class ConnectionProvider extends TConnectionProvider {

  override def createHbaseConnection(hConnConfig: Configuration): Connection = ConnectionFactory.createConnection(hConnConfig)

  override def createDatasourceConnection(mySQLProperties: Properties): DataSource = BasicDataSourceFactory.createDataSource(mySQLProperties)

  override def createCouchBaseConnection(nodes: List[String]): Cluster = CouchbaseCluster.create(nodes.asJava)
}
