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
package com.flipkart.connekt.commons.entities

import java.io.{File, FileInputStream, FileOutputStream, OutputStream}

import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.flipkart.connekt.commons.utils.StringUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.{StringUtils => ApacheStringUtils}

abstract class Credential extends Serializable

case class SimpleCredential(username: String, password: String) extends Credential {

  def isEmpty: Boolean = {
    StringUtils.isNullOrEmpty(username) && StringUtils.isNullOrEmpty(password)
  }

  def this() {
    this(null, null)
  }
}

case class AppleCredential(certificate: Array[Byte], passkey: String) extends Credential {

  @FieldSerializer.Optional(value = "AppleCredential.certificateFile")
  @volatile
  private var certificateFile: Option[File] = None

  def this(file: File, passkey: String) = {
    this({
      IOUtils.toByteArray(new FileInputStream(file))
    }, passkey)
    certificateFile = Some(file)
  }

  @throws[Exception]
  def getCertificateFile: File = {

    if (StringUtils.isNullOrEmpty(certificateFile))
      certificateFile = {
        val tempCertFile = File.createTempFile("apple_certificate", ".p12")
        tempCertFile.deleteOnExit()
        tempCertFile.setReadable(true, false)
        tempCertFile.setWritable(true, false)
        val output: OutputStream = new FileOutputStream(tempCertFile)
        IOUtils.write(certificate, output)
        Some(tempCertFile)
      }
    certificateFile.get
  }

  def this() {
    this(Array.emptyByteArray, null)
  }

}

object AppleCredential {
  def apply(file: File, passkey: String) = new AppleCredential(file, passkey)
}


case class MicrosoftCredential(clientId: String, clientSecret: String) extends Credential {

  def this() {
    this(null, null)
  }

}

case class GoogleCredential(projectId: String, apiKey: String) extends Credential {

  def this() {
    this(null, null)
  }

}

object Credentials {

  val EMPTY = SimpleCredential(ApacheStringUtils.EMPTY, ApacheStringUtils.EMPTY)


}
