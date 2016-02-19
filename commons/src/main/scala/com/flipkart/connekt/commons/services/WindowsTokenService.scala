package com.flipkart.connekt.commons.services

import java.net.URI
import java.util.concurrent.locks.ReentrantReadWriteLock

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.util.{ByteString, ByteStringBuilder}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.transmission.HostConnectionHelper._
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * @author aman.shrivastava on 15/02/16.
 */
case class Token(token: String, expectedExpiry: Long)

object WindowsTokenService {

  val cacheKey = "WINDOWS_TOKEN"

  val windowsURI = "https://login.live.com/accesstoken.srf"

  val platform = "windows"

  val uri = new URI(windowsURI).toURL
  implicit val clientPoolFlow = getPoolClientFlow[String]("login.live.com", 443)
  implicit val s = ActorSystem("foo")
  implicit val d = s.dispatcher
  val mat = ActorMaterializer

//  val secureId = ConnektConfig.getString("windows.clientId").getOrElse("ms-app://s-1-15-2-2528958255-2029194746-749418806-697355668-3484495234-315031297-461998615")
//  val clientSecret = ConnektConfig.getString("windows.clientSecret").getOrElse("0wYL2+lFYpHHD3nJS2Hoyh8gfpcmWFOV")
//  val windowsURI = ConnektConfig.getString("windows.access.token.endpoint").getOrElse("https://login.live.com/accesstoken.srf")

  val rwl = new ReentrantReadWriteLock()

  def getToken(appName: String): Option[Token] = {
    rwl.readLock().lock()
    try {
      LocalCacheManager.getCache[Token](LocalCacheType.WnsAccessToken).get(appName)
    } finally {
      rwl.readLock().unlock()
    }
  }

  def refreshToken(appName: String, requestTime: Long = System.currentTimeMillis()): Unit = {
    if (requestTime > getToken(appName).map(_.expectedExpiry).getOrElse(Long.MinValue))
      requestNewToken(appName, requestTime)
  }

  def requestNewToken(appName: String, requestTime: Long): Unit = {
    val credential = KeyChainManager.getMicrosoftCredential(appName)
    credential match {
      case Some(cred) =>
        if (rwl.writeLock().tryLock()) {
          try {
            ConnektLogger(LogFile.CLIENTS).debug("Windows token request")
            val postData = Map("grant_type" -> "client_credentials", "scope" -> "notify.windows.com", "client_id" -> cred.clientId, "client_secret" -> cred.clientSecret)

            val httpRequest = new HttpRequest(
              HttpMethods.POST,
              "/accesstoken.srf",
              scala.collection.immutable.Seq[HttpHeader](RawHeader("Content-Type", "application/x-www-form-urlencoded")),
              FormData(postData).toEntity
            )
            val fExec = request[String](httpRequest, "req-1")

            val responseBuilder = fExec.flatMap(r => r._1.map(_.entity.dataBytes.runFold[ByteStringBuilder](ByteString.newBuilder)((u, bs) => {u ++= bs})).get)
            val r = Await.result(responseBuilder.map(_.result().decodeString("UTF-8").getObj[ObjectNode]), 5.seconds)

            LocalCacheManager.getCache[Token](LocalCacheType.WnsAccessToken).put(appName, Token(r.get("access_token").asText(), r.get("expires_in").asLong * 1000 + requestTime))

          } finally {
            rwl.writeLock().unlock()
          }
        }
      case None =>
        ConnektLogger(LogFile.SERVICE).info(s"Cannot Invalid appName $appName")
    }
  }
}
