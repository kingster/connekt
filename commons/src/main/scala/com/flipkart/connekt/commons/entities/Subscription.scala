package com.flipkart.connekt.commons.entities

import java.util.Date

import com.flipkart.connekt.commons.utils.StringUtils._
import javax.persistence.Column

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind._

/**
  * Created by harshit.sinha on 07/06/16.
  */

class Subscription {

  @Column(name = "sId")
  var sId : String = _

  @Column(name = "sName")
  var sName : String = _

  @Column(name = "endpoint")
  var endpoint : Endpoint  = _

  @Column(name = "createdBy")
  var createdBy : String = _

  @Column(name = "createdTS")
  var createdTS: Date = _

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTS: Date = _

  @Column( name = "groovyString")
  var groovyString : String = _

  @Column( name = "shutdownThreshold")
  var shutdownThreshold : Int = _

  def this(sId: String, sName: String, endpoint: Endpoint ,
           createdBy: String, createdTS: Date, lastUpdatedTS: Date, groovyString: String, shutdownThreshold: Int) = {
    this
    this.sId = sId
    this.sName = sName
    this.endpoint = endpoint
    this.createdBy = createdBy
    this.createdTS = createdTS
    this.lastUpdatedTS = lastUpdatedTS
    this.groovyString = groovyString
    this.shutdownThreshold = shutdownThreshold
  }

  override def toString = s"Subscription($sId, $sName, ${endpoint.getJson}, $createdBy, ${createdTS.toString}," +
    s" ${lastUpdatedTS.toString}, $groovyString, ${shutdownThreshold.toString})"
}

