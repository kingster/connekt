package com.flipkart.connekt.commons.connections

import java.util.Properties
import javax.sql.DataSource

import com.couchbase.client.java.{Cluster, CouchbaseCluster}
import com.typesafe.config.Config
import org.apache.commons.dbcp2.BasicDataSourceFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{HConnectionManager, HConnection}

import scala.collection.JavaConverters._

/**
  * Created by kinshuk.bairagi on 27/01/16.
  */

class ConnectionProvider extends TConnectionProvider {

  override def createHbaseConnection(hConnConfig: Configuration): HConnection = HConnectionManager.createConnection(hConnConfig)

  override def createDatasourceConnection(mySQLProperties: Properties): DataSource = BasicDataSourceFactory.createDataSource(mySQLProperties)

  override def createCouchBaseConnection(nodes: List[String]): Cluster = CouchbaseCluster.create(nodes.asJava)
}