/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.connections

import java.util.Properties
import javax.sql.DataSource

import com.couchbase.client.java.Cluster
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.Connection

abstract class TConnectionProvider {

  def createCouchBaseConnection(nodes: List[String]): Cluster

  def createHbaseConnection(hConnConfig: Configuration) : Connection

  def createDatasourceConnection(mySQLProperties: Properties): DataSource
}
