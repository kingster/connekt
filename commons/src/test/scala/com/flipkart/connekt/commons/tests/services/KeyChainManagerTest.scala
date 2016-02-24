package com.flipkart.connekt.commons.tests.services

import java.io.{PrintWriter, File}

import com.flipkart.connekt.commons.entities.{AppleCredential, SimpleCredential}
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils

class KeyChainManagerTest extends CommonsBaseTest {

  val simpleName = "creds.simple." + StringUtils.generateRandomStr(6)
  val simpleCredentials = SimpleCredential("username", "password")

  val appleName = "creds.apple." + StringUtils.generateRandomStr(6)

  val fileName = StringUtils.generateRandomStr(7) + ".log"

  val file = new PrintWriter(new File("/tmp/" + fileName ))
  file.write(StringUtils.generateRandomStr(100))

  val appleCreds = AppleCredential(new File("/tmp/" + fileName), "passkey")

  "CredentialManagerTest" should "add/get simple " in {

    noException shouldBe thrownBy {
      KeyChainManager.addSimpleCredential(simpleName,simpleCredentials)
    }

    KeyChainManager.getSimpleCredential(simpleName).nonEmpty shouldEqual true
    KeyChainManager.getSimpleCredential(simpleName).get shouldEqual simpleCredentials

  }

  "CredentialManagerTest" should "add/get apple" in {
    noException shouldBe thrownBy {
      KeyChainManager.addAppleCredentials(appleName,appleCreds)
    }
    KeyChainManager.getAppleCredentials(appleName).nonEmpty shouldEqual true

    println(KeyChainManager.getAppleCredentials(appleName).get)

    KeyChainManager.getAppleCredentials(appleName).get.passkey shouldEqual appleCreds.passkey

    println("Certificate FILE ======> "+  KeyChainManager.getAppleCredentials(appleName).get.getCertificateFile)

    val x = KeyChainManager.getAppleCredentials(appleName).get.getCertificateFile
    x should not be appleCreds.getCertificateFile

  }

}
