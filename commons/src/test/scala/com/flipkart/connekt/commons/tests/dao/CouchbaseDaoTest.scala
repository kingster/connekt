package com.flipkart.connekt.commons.tests.dao

import java.util.UUID

import com.couchbase.client.deps.io.netty.buffer.Unpooled
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.document.{BinaryDocument, JsonDocument, StringDocument}
import com.couchbase.client.java.{Bucket, Cluster}
import com.flipkart.connekt.commons.tests.connections.couchbase.CouchbaseMockCluster
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.connekt.commons.utils.StringUtils
import org.scalatest.Ignore

/**
 *
 *
 * @author durga.s
 * @version 1/18/16
 */
@Ignore
class CouchbaseDaoTest extends ConnektUTSpec {

  private var cluster: Cluster = null
  private var insertId1: String = UUID.randomUUID().toString
  private var insertId2: String = UUID.randomUUID().toString
  private var insertId3: String = UUID.randomUUID().toString

  override def beforeAll() = {
    super.beforeAll()
    createClusterConn()
  }

  override def afterAll() = {
    Option(cluster).map(_.disconnect())
    super.afterAll()
  }

  "Put operation" should "add a new doc" in {
    val bucket: Bucket = cluster.openBucket("default")
    assert(null != bucket.insert(getSampleDocument))
  }

  "Fetch operation" should "get an existing doc" in {
    val bucket: Bucket = cluster.openBucket("default")
    val document = bucket.get(insertId1)
    println(document)
    assert(null != document)
  }

  "PUT" should "add string" in {
    val bucket: Bucket = cluster.openBucket("default")
    assert(null != bucket.insert(StringDocument.create(insertId2, "214354t3w34")))
  }

  "FETCH" should "get string" in {
    val bucket: Bucket = cluster.openBucket("default")
    val document = bucket.get(StringDocument.create(insertId2))
    println(document)
    assert(null != document)
  }

  "PUT" should "add bytes" in {
    val bucket: Bucket = cluster.openBucket("default")
    assert(null != bucket.insert(BinaryDocument.create(insertId3,Unpooled.wrappedBuffer("sadasdsad".getBytes))))
  }

  "FETCH" should "get bytes" in {
    val bucket: Bucket = cluster.openBucket("default")
    val document = bucket.get(BinaryDocument.create(insertId3))
    println(new String(document.content().array()))
    assert(null != document)
  }


  private def getSampleDocument: JsonDocument = {

    val document = JsonObject.empty()
      .put("state", "login")
      .put("model", "SM-G530H")
      .put("token", s"APA91bGUpvddvIG4rtlf_XR12M79EclmGyWIDv0Gkwj9DpEQbmei5RvWcmFxNBCF3ZBFBgRcbV_${StringUtils.generateSecureRandom}")
      .put("brand", "samsung")
      .put("appVersion", "590206")
      .put("osVersion", "4.4.4")
      .put("userId", s"ACC$insertId1")

    JsonDocument.create(insertId1, document)
  }

  private def createClusterConn() = {
    //cluster = CouchbaseCluster.create("10.32.113.29", "10.33.125.53")
    cluster = new CouchbaseMockCluster
  }
}
