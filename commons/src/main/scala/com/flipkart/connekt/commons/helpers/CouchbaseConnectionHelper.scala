package com.flipkart.connekt.commons.helpers

import com.couchbase.client.java.CouchbaseCluster
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
 * Created by nidhi.mehla on 21/01/16.
 */
object CouchbaseConnectionHelper {

  def createCouchBaseConnection(cf: Config):CouchbaseCluster = {
    CouchbaseCluster.create(cf.getString("clusterIpList").split(",").toList.asJava)
  }

}
