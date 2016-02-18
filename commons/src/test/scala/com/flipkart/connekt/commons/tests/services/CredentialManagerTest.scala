package com.flipkart.connekt.commons.tests.services

import java.io.File

import com.flipkart.connekt.commons.entities.{AppleCredential, SimpleCredential}
import com.flipkart.connekt.commons.services.CredentialManager
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils

class CredentialManagerTest extends CommonsBaseTest {

  val simpleName = "creds.simple." + StringUtils.generateRandomStr(6)
  val simpleCredentials = SimpleCredential("username", "password")

  val appleName = "creds.apple." + StringUtils.generateRandomStr(6)
  val appleCreds = AppleCredential(new File("/Users/kinshuk.bairagi/Documents/work/comm/connekt/velocity.log"), "passkey")

  "CredentialManagerTest" should "add/get simple " in {

    noException shouldBe thrownBy {
      CredentialManager.addSimpleCredential(simpleName,simpleCredentials)
    }

    CredentialManager.getSimpleCredential(simpleName).nonEmpty shouldEqual true
    CredentialManager.getSimpleCredential(simpleName).get shouldEqual simpleCredentials

  }

  "CredentialManagerTest" should "add/get apple" in {
    noException shouldBe thrownBy {
      CredentialManager.addAppleCredentials(appleName,appleCreds)
    }
    CredentialManager.getAppleCredentials(appleName).nonEmpty shouldEqual true

    println(CredentialManager.getAppleCredentials(appleName).get)

    CredentialManager.getAppleCredentials(appleName).get.passkey shouldEqual appleCreds.passkey

    println("Certificate FILE ======> "+  CredentialManager.getAppleCredentials(appleName).get.getCertificateFile)

    CredentialManager.getAppleCredentials(appleName).get.getCertificateFile should not be appleCreds.getCertificateFile

  }

}
