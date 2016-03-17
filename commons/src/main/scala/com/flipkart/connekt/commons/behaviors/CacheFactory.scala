/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.behaviors

import com.couchbase.client.java.document.JsonDocument

trait CacheFactory {

  def insert(doc: JsonDocument): String
  def fetch(id: String): JsonDocument

}
