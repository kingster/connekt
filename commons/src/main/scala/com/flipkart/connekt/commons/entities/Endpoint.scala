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
  new Type(value = classOf[HTTPEndpoint], name = "HTTP"),
  new Type(value = classOf[KafkaEndpoint], name = "KAFKA")
))
abstract class Endpoint extends JSONField
case class HTTPEndpoint(url: String) extends Endpoint
case class KafkaEndpoint(zookeeper: String, broker: String) extends Endpoint
