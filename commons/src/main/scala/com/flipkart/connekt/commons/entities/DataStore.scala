package com.flipkart.connekt.commons.entities

import java.util.Date
import javax.persistence.Column

/**
 * Created by nidhi.mehla on 17/02/16.
 */
class DataStore {
  @Column(name = "key")
  var key: String = _

  @Column(name = "type")
  var `type`: String = _

  @Column(name = "value")
  var value: Array[Byte] = _

  @Column(name = "creationTS")
  var creationTS: Date = _

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTS: Date = _

  def this(key: String,
           `type`: String,
           value: Array[Byte],
           creationTS: Date,
           lastUpdatedTS: Date
            ) {
    this()
    this.key = key
    this.`type` = `type`
    this.value = value
    this.creationTS = creationTS
    this.lastUpdatedTS = lastUpdatedTS
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AppUser]

  override def equals(other: Any): Boolean = other match {
    case that: DataStore =>
      (that canEqual this) &&
        key == that.key &&
        value == that.value &&
        creationTS == that.creationTS &&
        lastUpdatedTS == that.lastUpdatedTS
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(key, value, creationTS, lastUpdatedTS)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }


}
