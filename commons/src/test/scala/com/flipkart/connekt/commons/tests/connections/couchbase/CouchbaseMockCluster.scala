package com.flipkart.connekt.commons.tests.connections.couchbase

import java.lang.Boolean
import java.util
import java.util.concurrent.TimeUnit

import com.couchbase.client.core.{CouchbaseCore, ClusterFacade}
import com.couchbase.client.java.cluster.ClusterManager
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment
import com.couchbase.client.java.{Bucket, Cluster}
import com.couchbase.client.java.document.Document
import com.couchbase.client.java.transcoder.Transcoder

class CouchbaseMockCluster extends Cluster {

  private var buckets: Map[String, Bucket] = Map()

  private val couchbaseEnvironment = DefaultCouchbaseEnvironment.create()

  override def disconnect(): Boolean = true

  override def disconnect(l: Long, timeUnit: TimeUnit): Boolean = true

  override def clusterManager(s: String, s1: String): ClusterManager = ???

  override def openBucket(): Bucket = ???

  override def openBucket(l: Long, timeUnit: TimeUnit): Bucket = ???

  override def openBucket(bucketName: String): Bucket = this.synchronized {
    buckets.get(bucketName) match {
      case Some(x) => x
      case None =>
        val bucket = new CouchbaseMockBucket(couchbaseEnvironment, new CouchbaseCore(couchbaseEnvironment), bucketName)
        buckets += bucketName -> bucket
        bucket
    }
  }

  override def openBucket(s: String, l: Long, timeUnit: TimeUnit): Bucket = ???

  override def openBucket(s: String, s1: String): Bucket = ???

  override def openBucket(s: String, s1: String, l: Long, timeUnit: TimeUnit): Bucket = ???

  override def openBucket(s: String, s1: String, list: util.List[Transcoder[_ <: Document[_], _]]): Bucket = ???

  override def openBucket(s: String, s1: String, list: util.List[Transcoder[_ <: Document[_], _]], l: Long, timeUnit: TimeUnit): Bucket = ???

  override def core(): ClusterFacade = ???
}
