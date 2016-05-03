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
package com.flipkart.connekt.commons.services

import java.util
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.utils.http.HttpClient
import com.flipkart.utils.http.HttpClient._
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair

case class OAuthToken(token: String, expectedExpiry: Long)

trait TWindowsOAuthService {
  def getToken(appName: String): Option[OAuthToken]
  def refreshToken(appName: String, requestTime: Long = System.currentTimeMillis())
}

object WindowsOAuthService extends TWindowsOAuthService {

  val tokenRefreshURI = ConnektConfig.getString("windows.access.token.endpoint").getOrElse("https://login.live.com/accesstoken.srf")

  val client = new HttpClient(
    name = "windows-token-client",
    ttlInMillis = ConnektConfig.getInt("http.apache.ttlInMillis").getOrElse(60000).toLong,
    maxConnections = ConnektConfig.getInt("http.apache.maxConnections").getOrElse(100),
    processQueueSize = ConnektConfig.getInt("http.apache.processQueueSize").getOrElse(100),
    connectionTimeoutInMillis = ConnektConfig.getInt("http.apache.connectionTimeoutInMillis").getOrElse(30000).toInt,
    socketTimeoutInMillis = ConnektConfig.getInt("http.apache.socketTimeoutInMillis").getOrElse(60000).toInt
  )

  val rwl = new ReentrantReadWriteLock()

  def getToken(appName: String): Option[OAuthToken] = {
    rwl.readLock().lock()
    try {
      LocalCacheManager.getCache[OAuthToken](LocalCacheType.WnsAccessToken).get(appName.trim.toLowerCase)
    } finally {
      rwl.readLock().unlock()
    }
  }

  def refreshToken(appName: String, requestTime: Long = System.currentTimeMillis()): Unit = {
    val tokenExpiryTime = getToken(appName.trim.toLowerCase).map(_.expectedExpiry).getOrElse(Long.MinValue)
    if (requestTime > tokenExpiryTime)
      requestNewToken(appName, requestTime)
    else
      ConnektLogger(LogFile.CLIENTS).info(s"Windows token request for App $appName dropped since requestTime [$requestTime] < tokenExpiryTime [$tokenExpiryTime]")
  }

  private def requestNewToken(appName: String, requestTime: Long): Unit = {
    val credential = KeyChainManager.getMicrosoftCredential(appName)
    credential match {
      case Some(cred) =>
        if (rwl.writeLock().tryLock()) {
          try {
            ConnektLogger(LogFile.CLIENTS).info(s"Windows token request for App $appName")

            val request = new HttpPost(tokenRefreshURI)
            val params = new util.ArrayList[NameValuePair]()
            params.add(new BasicNameValuePair("grant_type", "client_credentials"))
            params.add(new BasicNameValuePair("scope", "notify.windows.com"))
            params.add(new BasicNameValuePair("client_id", cred.clientId))
            params.add(new BasicNameValuePair("client_secret",cred.clientSecret))
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"))

            val responseString = client.doExecute(request)
            val response = responseString.get.getObj[ObjectNode]

            ConnektLogger(LogFile.CLIENTS).info(s"Windows token request response for App $appName Response $response")

            LocalCacheManager.getCache[OAuthToken](LocalCacheType.WnsAccessToken).put(appName.trim.toLowerCase, OAuthToken(response.get("access_token").asText(), response.get("expires_in").asLong * 1000 + requestTime))

            ConnektLogger(LogFile.CLIENTS).info(s"Windows token updated for App $appName")

          } finally {
            rwl.writeLock().unlock()
          }
        }
      case None =>
        ConnektLogger(LogFile.SERVICE).error(s"Cannot requestNewToken Invalid appName $appName")
    }
  }
}
