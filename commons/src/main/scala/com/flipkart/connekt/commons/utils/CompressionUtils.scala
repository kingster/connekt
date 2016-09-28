/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.util.Base64
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.fasterxml.jackson.annotation.JsonProperty
import com.flipkart.connekt.commons.serializers.KryoSerializer
import org.apache.commons.io.IOUtils

import scala.util.Try
import com.flipkart.connekt.commons.core.Wrappers._
/**
  * Created by kinshuk.bairagi on 27/07/16.
  */
object CompressionUtils {

  def inflate(deflatedTxt: String): Try[String] = Try_ {
    val bytes = Base64.getUrlDecoder.decode(deflatedTxt)
    val zipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))
    IOUtils.toString(zipInputStream)
  }

  def deflate(txt: String): Try[String] = Try_ {
    val arrOutputStream = new ByteArrayOutputStream()
    val zipOutputStream = new GZIPOutputStream(arrOutputStream)
    zipOutputStream.write(txt.getBytes)
    zipOutputStream.close()
    Base64.getUrlEncoder.encodeToString(arrOutputStream.toByteArray)
  }

  implicit class StringCompress(val s: String) {
    def compress = deflate(s)
  }

/*
  import StringUtils._
  def main(args: Array[String]): Unit = {
    val obj = HelloWorld("16C51516B3A6413FA0B90D1F81EE5F9B", "ff5cb72e-29ce-4a3c-94a8-d582841cceaf","Ceryx","EM-MPN1234567890","RetailApp")
    println(obj.getJson)
    println(Base64.getUrlEncoder.encodeToString(obj.getJson.getBytes))
    val d = obj.getJson.compress.get
    println(d)
    println(inflate(d))
  }

  case class HelloWorld(@JsonProperty("dId") deviceId:String,@JsonProperty("mId") messageId:String,@JsonProperty("cId") clientId:String,@JsonProperty("ctx") contextId:String,@JsonProperty("aId") appId:String)
*/

}
