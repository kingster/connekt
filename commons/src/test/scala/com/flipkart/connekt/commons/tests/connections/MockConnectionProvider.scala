package com.flipkart.connekt.commons.tests.connections

import java.util.Properties
import javax.sql.DataSource

import com.couchbase.client.java.Cluster
import com.flipkart.connekt.commons.connections.TConnectionProvider
import com.flipkart.connekt.commons.tests.connections.couchbase.CouchbaseMockCluster
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{HConnection, HConnectionManager}

/**
  * Created by kinshuk.bairagi on 27/01/16.
  */
class MockConnectionProvider extends TConnectionProvider{

  override def createCouchBaseConnection(nodes: List[String]): Cluster = new CouchbaseMockCluster

  override def createHbaseConnection(hConnConfig: Configuration): HConnection = HConnectionManager.createConnection(hConnConfig)

  override def createDatasourceConnection(mySQLProperties: Properties): DataSource = {
    val ds = new BasicDataSource()
    ds.setDriverClassName("org.h2.Driver")
    ds.setUrl( "jdbc:h2:~/test;MODE=MySQL" )
    ds
  }

}
