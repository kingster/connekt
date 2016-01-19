package com.flipkart.connekt.commons.entities

import javax.persistence.Column

import com.fasterxml.jackson.core.{JsonParser, JsonGenerator}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, SerializerProvider, JsonSerializer}
import com.flipkart.connekt.commons.entities.StencilEngine.StencilEngine

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
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

  def this(id: String, engine: StencilEngine, engineFabric: String) = {
    this
    this.id = id
    this.engine = engine
    this.engineFabric = engineFabric
  }
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
      StencilEngine.withName(parser.getValueAsString)
    } catch {
      case e: NoSuchElementException =>
        null
    }
  }
}

