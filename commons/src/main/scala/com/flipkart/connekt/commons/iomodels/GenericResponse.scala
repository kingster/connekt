package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

/**
 *
 *
 * @author durga.s
 * @version 11/21/15
 */
case class GenericResponse(status: Int, request: AnyRef, response: ResponseBody)

@JsonTypeInfo(
use = JsonTypeInfo.Id.NAME,
include = JsonTypeInfo.As.PROPERTY,
property = "type"
)
@JsonSubTypes(Array(
new Type(value = classOf[Response], name = "RESPONSE"),
new Type(value = classOf[MulticastResponse], name = "MULTICAST_RESPONSE")
))
abstract class ResponseBody

case class Response(message: String, data: Any) extends ResponseBody

case class MulticastResponse(message: String, success: Map[String, List[String]], failure: List[String]) extends ResponseBody
