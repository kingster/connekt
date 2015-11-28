package com.flipkart.connekt.commons.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.commons.codec.CharEncoding

import scala.reflect.ClassTag
import scala.reflect._
/**
 *
 *
 * @author durga.s
 * @version 11/23/15
 */
object StringUtils {

  implicit class StringHandyFunctions(val s: String) {
    def getUtf8Bytes = s.getBytes(CharEncoding.UTF_8)
  }

  private val objMapper = new ObjectMapper() with ScalaObjectMapper
  objMapper.registerModules(Seq(DefaultScalaModule):_*)

  implicit class JSONMarshallFunctions(val o: AnyRef) {
    def getJson = objMapper.writeValueAsString(o)
  }
  
  implicit class JSONUnMarshallFunctions(val s: String) {
    def getObj[T: ClassTag] = objMapper.readValue(s, classTag[T].runtimeClass).asInstanceOf[T]
  }
}
