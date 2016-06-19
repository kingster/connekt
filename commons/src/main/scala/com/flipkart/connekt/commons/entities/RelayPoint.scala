package com.flipkart.connekt.commons.entities

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.flipkart.connekt.commons.dao.JSONField

/**
  * Created by harshit.sinha on 10/06/16.
  **/


@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(Array(
  new Type(value = classOf[HTTPRelayPoint], name = "HTTP"),
  new Type(value = classOf[KafkaRelayPoint], name = "KAFKA")
))
abstract class RelayPoint extends JSONField
case class HTTPRelayPoint(url: String) extends RelayPoint
case class KafkaRelayPoint(zookeeper: String, broker: String) extends RelayPoint
