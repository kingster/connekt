/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.services

import scala.util.Try

trait TStorageService extends TService {

  def put(key: String, value: String): Try[Unit]

  def put(key: String, value: Array[Byte]): Try[Unit]

  def get(key: String): Try[Option[Array[Byte]]]


}
