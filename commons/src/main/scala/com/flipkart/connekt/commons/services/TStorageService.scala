package com.flipkart.connekt.commons.services

import scala.util.Try

/**
 * Created by nidhi.mehla on 18/02/16.
 */
trait TStorageService extends TService {

  def put(key: String, value: String): Try[Unit]

  def put(key: String, value: Array[Byte]): Try[Unit]

  def get(key: String): Try[Option[Array[Byte]]]


}
