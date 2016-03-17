/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.receptors.wire

import akka.http.scaladsl.marshalling.{PredefinedToEntityMarshallers, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
 * Derives on [[akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers]] and [[akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers]]
 * to provide implicit generic json un/marshallers. As per akka-http documentation `akka-http-spray-json`
 * module can be used along-with RootJsonReader/RootJsonWriter implementations for every model type T. <br>
 *
 * This however, relies on [[https://github.com/FasterXML/jackson-module-scala scala-jackson]]
 * and hence, not using [[http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0-M1/scala/http/common/json-support.html akka-http-spray-json]]
 * <br>
 *
 * Gist Attribution [[https://gist.github.com/chadselph Chad Selph]]
 * @see
 * http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0-M1/scala/http/common/marshalling.html
 * http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0-M1/scala/http/common/unmarshalling.html
 *
 */
trait GenericJsonSupport {

  val jacksonModules = Seq(DefaultScalaModule)

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModules(jacksonModules: _*)
}

trait JsonToEntityMarshaller extends GenericJsonSupport with PredefinedToEntityMarshallers {

  implicit def genericMarshaller[T <: AnyRef]: ToEntityMarshaller[T] =
    stringMarshaller(MediaTypes.`application/json`)
      .compose[T](mapper.writeValueAsString)
}

trait JsonFromEntityUnmarshaller extends GenericJsonSupport with PredefinedFromEntityUnmarshallers {

  implicit def genericUnmarshaller[T: Manifest]: FromEntityUnmarshaller[T] =
    stringUnmarshaller.forContentTypes(MediaTypes.`application/json`)
      .map(mapper.readValue[T])
}
