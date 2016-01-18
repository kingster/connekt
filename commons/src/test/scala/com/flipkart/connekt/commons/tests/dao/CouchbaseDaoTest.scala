package com.flipkart.connekt.commons.tests.dao

import java.util.UUID

import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.{Bucket, CouchbaseCluster}
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

  private var cluster: CouchbaseCluster = null
  private var insertId: String = UUID.randomUUID().toString

  override def beforeAll() = {
    super.beforeAll()
    createClusterConn()
  }

  override def afterAll() = {
    Some(cluster).map(_.disconnect())
    super.afterAll()
  }

  "Put operation" should "add a new doc" in {
    val bucket: Bucket = cluster.openBucket("default")
    assert(null != bucket.insert(getSampleDocument))
  }

  "Fetch operation" should "get an existing doc" in {
    val bucket: Bucket = cluster.openBucket("default")
    val document = bucket.get(insertId)
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
      .put("userId", s"ACC$insertId")

    JsonDocument.create(insertId, document)
  }

  private def createClusterConn() = {
    cluster = CouchbaseCluster.create("10.32.113.29", "10.33.125.53")
  }
}
