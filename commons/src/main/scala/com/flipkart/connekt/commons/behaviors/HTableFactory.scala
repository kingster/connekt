/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.behaviors

import org.apache.hadoop.hbase.client.Table


trait HTableFactory {
  def getTableInterface(tableName: String): Table
  def releaseTableInterface(hTableInterface: Table)
  def shutdown()
}
