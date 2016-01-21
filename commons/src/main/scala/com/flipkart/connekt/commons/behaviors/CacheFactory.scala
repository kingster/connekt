package com.flipkart.connekt.commons.behaviors

import com.couchbase.client.java.document.JsonDocument

/**
 * Created by nidhi.mehla on 20/01/16.
 */
trait CacheFactory {

  def insert(doc: JsonDocument): String
  def fetch(id: String): JsonDocument

}
