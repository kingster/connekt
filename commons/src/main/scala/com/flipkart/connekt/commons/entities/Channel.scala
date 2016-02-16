package com.flipkart.connekt.commons.entities

import com.fasterxml.jackson.core.{JsonParser, JsonGenerator}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, SerializerProvider, JsonSerializer}

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
object Channel extends Enumeration {
  type Channel = Value
  val PUSH = Value("push")
  val EMAIL = Value("email")
  val SMS = Value("sms")
  val CARDS = Value("cards")
  val OPENWEB = Value("openweb")
}

class ChannelToStringSerializer extends JsonSerializer[Channel.Value] {
  override def serialize(t: Channel.Value, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) = {
    jsonGenerator.writeObject(t.toString)
  }
}

class ChannelToStringDeserializer extends JsonDeserializer[Channel.Value] {
  @Override
  override def deserialize(parser:JsonParser, context:DeserializationContext):Channel.Value={
    try {
      com.flipkart.connekt.commons.entities.Channel.withName(parser.getValueAsString)
    } catch {
      case e: NoSuchElementException =>
        null
    }
  }
}