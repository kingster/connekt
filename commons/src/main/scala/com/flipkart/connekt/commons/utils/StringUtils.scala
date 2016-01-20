package com.flipkart.connekt.commons.utils

import java.math.BigInteger
import java.security.{SecureRandom, MessageDigest}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.commons.codec.CharEncoding

import scala.reflect.ClassTag
import scala.reflect._
import NullWrapper._
/**
 *
 *
 * @author durga.s
 * @version 11/23/15
 */
object StringUtils {

  implicit class StringHandyFunctions(val s: String) {
    def getUtf8Bytes = s.getBytes(CharEncoding.UTF_8)

    def getUtf8BytesNullWrapped = Option(s).map(_.getUtf8Bytes).orNull.wrap

  }


  implicit class ByteArrayHandyFunctions(val b: Array[Byte]) {
    def getString = new String(b, CharEncoding.UTF_8)

    def getStringNullable = b.unwrap match {
      case array if array.isEmpty => null
      case value => new String(value, CharEncoding.UTF_8)
    }

  }

  val objMapper = new ObjectMapper() with ScalaObjectMapper
  objMapper.registerModules(Seq(DefaultScalaModule):_*)

  implicit class JSONMarshallFunctions(val o: AnyRef) {
    def getJson = objMapper.writeValueAsString(o)
  }
  
  implicit class JSONUnMarshallFunctions(val s: String) {
    def getObj[T: ClassTag] = objMapper.readValue(s, classTag[T].runtimeClass).asInstanceOf[T]
    def getObj(implicit cType: Class[_]) = objMapper.readValue(s, cType)
  }

  def isNullOrEmpty(o: Any): Boolean = o match {
    case m: Map[_, _] => m.isEmpty
    case i: Iterable[Any] => i.isEmpty
    case null | None | "" => true
    case Some(x) => isNullOrEmpty(x)
    case _ => false
  }

  def getObjectNode = objMapper.createObjectNode()

  def getArrayNode = objMapper.createArrayNode()

  def md5(s: String) : String = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(s.getBytes)
    md5.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
  }

  def generateRandomStr(len: Int): String = {
    val ZERO = Character.valueOf('0')
    val A = Character.valueOf('A')
    val sb = new StringBuffer()
    for (i <- 1 to len) {
      var n = (36.0 * Math.random).asInstanceOf[Int]
      if (n < 10) {
        n = (ZERO + n)
      }
      else {
        n -= 10
        n = (A + n)
      }
      sb.append(new Character(n.asInstanceOf[Char]))
    }
    return new String(sb)
  }

  def generateSecureRandom:String = {
    val random:SecureRandom = new SecureRandom()
    new BigInteger(130, random).toString(32)
  }

}
