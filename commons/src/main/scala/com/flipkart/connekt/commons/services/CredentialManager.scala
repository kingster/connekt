package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.entities._
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.serializers.KryoSerializer


/**
 * Created by kinshuk.bairagi on 13/11/14.
 */

object CredentialManager {

  private val storage = ServiceFactory.getStorageService

  def addSimpleCredential(name: String, credential: SimpleCredential) = {
    val bytes = KryoSerializer.serialize(credential)
    storage.put(name, bytes)
  }

  @throws[Exception]
  def getSimpleCredential(name: String): Option[SimpleCredential] = {
    storage.get(name).get.map(KryoSerializer.deserialize[SimpleCredential])
  }

  def addAppleCredentials(name: String, credential: AppleCredential) = {
    val bytes = KryoSerializer.serialize(credential)
    storage.put(name, bytes)
  }

  @throws[Exception]
  def getAppleCredentials(name: String): Option[AppleCredential] = {
    storage.get(name).get.map(KryoSerializer.deserialize[AppleCredential])
  }

  def addMicrosoftCredential(name: String, credential: MicrosoftCredential) = {
    val bytes = KryoSerializer.serialize(credential)
    storage.put(name, bytes)
  }

  @throws[Exception]
  def getMicrosoftCredential(name: String): Option[MicrosoftCredential] = {
    storage.get(name).get.map(KryoSerializer.deserialize[MicrosoftCredential])
  }

  def addGoogleCredential(name: String, credential: GoogleCredential) = {
    val bytes = KryoSerializer.serialize(credential)
    storage.put(name, bytes)
  }

  @throws[Exception]
  def getGoogleCredential(name: String): Option[GoogleCredential] = {
    storage.get(name).get.map(KryoSerializer.deserialize[GoogleCredential])
  }


}