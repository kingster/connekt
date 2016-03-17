/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.behaviors

import org.apache.hadoop.hbase.client.HTableInterface

trait HTableFactory {
  def getTableInterface(tableName: String): HTableInterface
  def releaseTableInterface(hTableInterface: HTableInterface)
  def shutdown()
}
