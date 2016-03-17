/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities

import java.util.Date
import javax.persistence.Column

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import com.flipkart.connekt.commons.entities.StencilEngine.StencilEngine

class Stencil() {
  @Column(name = "id")
  var id: String = _

  @EnumTypeHint(value = "com.flipkart.connekt.commons.entities.StencilEngine")
  @Column(name = "engine")
  @JsonSerialize(using = classOf[StencilEngineToStringSerializer])
  @JsonDeserialize(using = classOf[StencilEngineToStringDeserializer])
  var engine: StencilEngine.StencilEngine = StencilEngine.GROOVY

  @Column(name = "engineFabric")
  var engineFabric: String = _

  @Column(name = "createdBy")
  var createdBy: String = _


  @Column(name = "updatedBy")
  var updatedBy: String = _

  @Column(name = "version")
  var version: Int = 1

  @Column(name = "creationTS")
  var creationTS: Date = _

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTS: Date = _

  @Column(name = "bucket")
  var bucket: String = _


  def this(id: String, engine: StencilEngine, engineFabric: String) = {
    this
    this.id = id
    this.engine = engine
    this.engineFabric = engineFabric
    this.bucket = bucket
  }

  override def toString = s"Stencil($id, $engine, $engineFabric)"
}

object StencilEngine extends Enumeration {
  type StencilEngine = Value
  val VELOCITY, GROOVY = Value
}

class StencilEngineToStringSerializer extends JsonSerializer[StencilEngine] {
  override def serialize(t: StencilEngine.Value, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) = {
    jsonGenerator.writeObject(t.toString)
  }
}

class StencilEngineToStringDeserializer extends JsonDeserializer[StencilEngine] {
  @Override
  override def deserialize(parser:JsonParser, context:DeserializationContext):StencilEngine.Value={
    try {
      StencilEngine.withName(parser.getValueAsString.toUpperCase)
    } catch {
      case e: NoSuchElementException =>
        null
    }
  }
}
