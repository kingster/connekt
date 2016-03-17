/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.connections

import java.util.Properties
import javax.sql.DataSource

import com.couchbase.client.java.Cluster
import com.typesafe.config.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.HConnection

abstract class TConnectionProvider {

  def createCouchBaseConnection(nodes: List[String]): Cluster

  def createHbaseConnection(hConnConfig: Configuration) : HConnection

  def createDatasourceConnection(mySQLProperties: Properties): DataSource
}
