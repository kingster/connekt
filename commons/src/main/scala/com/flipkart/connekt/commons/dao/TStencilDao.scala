/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.{Bucket, Stencil}

trait TStencilDao extends Dao {

  def getStencil(id: String, version: Option[String] = None): Option[Stencil]
//  def updateStencil(stencil: Stencil): Unit
  def writeStencil(stencil: Stencil): Unit
  def getBucket(name: String): Option[Bucket]
  def writeBucket(bucket: Bucket): Unit
}
