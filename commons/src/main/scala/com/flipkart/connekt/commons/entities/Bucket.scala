/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities

import javax.persistence.Column

/**
 * @author aman.shrivastava on 27/01/16.
 */
class Bucket {
  @Column(name = "name")
  var name: String = _

  @Column(name = "id")
  var id: String = _

  override def toString = s"Bucket($name, $id)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[Bucket]

  override def equals(other: Any): Boolean = other match {
    case that: Bucket =>
      (that canEqual this) &&
        name == that.name &&
        id == that.id
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(name, id)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
