package com.flipkart.connekt.commons.services

/**
 * Created by nidhi.mehla on 18/02/16.
 */
trait TStorageService extends TService {

  def put(key: String, value: String): Boolean

  def put(key: String, value: Array[Byte]): Boolean

  def get(key: String): Option[Array[Byte]]
}
