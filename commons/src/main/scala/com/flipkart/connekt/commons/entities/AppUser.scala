package com.flipkart.connekt.commons.entities

import java.util.Date
import javax.persistence.Column

import org.apache.commons.lang.StringUtils

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
class AppUser {

  @Column(name = "userId")
  var userId: String = _

  @Column(name = "apikey")
  var apiKey: String = _

  @Column(name = "groups")
  var groups: String = _

  @Column(name = "contact")
  var contact: String = _

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTs: Date = new Date(System.currentTimeMillis())

  @Column(name = "updatedBy")
  var updatedBy: String = StringUtils.EMPTY

  def this(userId: String,
           apiKey: String,
           groups: String,
           contact: String
           ) {
    this()
    this.userId = userId
    this.apiKey = apiKey
    this.groups = groups
    this.contact = contact
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AppUser]

  override def equals(other: Any): Boolean = other match {
    case that: AppUser =>
      (that canEqual this) &&
        userId == that.userId &&
        apiKey == that.apiKey &&
        groups == that.groups &&
        updatedBy == that.updatedBy
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(userId, apiKey, groups, updatedBy)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
