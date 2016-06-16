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
package com.flipkart.connekt.busybees.streams.flows.formaters

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.encryption.WebPushEncryptionUtils
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.busybees.streams.flows.NIOFlow
import com.flipkart.connekt.commons.entities.MobilePlatform
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.CallbackRecorder._
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.{DeviceDetailsService, KeyChainManager, StencilService}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class OpenWebChannelFormatter(parallelism: Int)(implicit ec: ExecutionContextExecutor) extends NIOFlow[ConnektRequest, OpenWebStandardPayloadEnvelope](parallelism)(ec) {

  override def map: ConnektRequest => List[OpenWebStandardPayloadEnvelope] = message => {

    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"OpenWebChannelFormatter received message: ${message.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"OpenWebChannelFormatter received message: ${message.toString}")

      val pnInfo = message.channelInfo.asInstanceOf[PNRequestInfo]

      val devicesInfo = DeviceDetailsService.get(pnInfo.appName, pnInfo.deviceIds).get
      val validDeviceIds = devicesInfo.map(_.deviceId)
      val invalidDeviceIds = pnInfo.deviceIds.diff(validDeviceIds.toSet)
      invalidDeviceIds.map(PNCallbackEvent(message.id, message.clientId, _, InternalStatus.MissingDeviceInfo, MobilePlatform.OPENWEB, pnInfo.appName, message.contextId.orEmpty)).persist

      val ttl = message.expiryTs.map(expiry => (expiry - System.currentTimeMillis) / 1000).getOrElse(6.hour.toSeconds)
      val openWebStencil = StencilService.getStencilByName(s"ckt-${pnInfo.appName.toLowerCase}-openweb").get.headOption.orNull

      if (ttl > 0) {
        devicesInfo.flatMap(device => {
          //standard open-web (firefox, chrome, etc)
          if (device.token != null && device.token.nonEmpty && device.token.isValidUrl) {
            val token = device.token.replace("https://android.googleapis.com/gcm/send", "https://gcm-http.googleapis.com/gcm")
            val headers = scala.collection.mutable.Map("TTL" -> ttl.toString)
            val appDataWithId = StencilService.render(openWebStencil, message.channelData.asInstanceOf[PNRequestData].data).asInstanceOf[String].getObj[ObjectNode].put("messageId", message.id).getJson

            if (device.token.startsWith("https://android.googleapis.com/gcm/send"))
              headers += ("Authorization" -> s"key=${KeyChainManager.getGoogleCredential(message.appName).get.apiKey}")

            if (device.keys != null && device.keys.nonEmpty) {
              Try(WebPushEncryptionUtils.encrypt(device.keys("p256dh"), device.keys("auth"), appDataWithId)) match {
                case Success(data) =>
                  headers +=(
                    "Crypto-Key" -> WebPushEncryptionUtils.createCryptoKeyHeader(data.serverPublicKey),
                    "Encryption" -> WebPushEncryptionUtils.createEncryptionHeader(data.salt),
                    "Content-Encoding" -> "aesgcm"
                    )
                  List(OpenWebStandardPayloadEnvelope(message.id, message.clientId, device.deviceId, pnInfo.appName,
                    message.contextId.orEmpty, token, OpenWebStandardPayload(data.encodedData), headers.toMap, message.meta))
                case Failure(e) =>
                  ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebChannelFormatter error for ${message.id}", e)
                  PNCallbackEvent(message.id, message.clientId, device.deviceId, InternalStatus.EncryptionError, MobilePlatform.OPENWEB, device.appName, message.contextId.orEmpty, e.getMessage).persist
                  List.empty[OpenWebStandardPayloadEnvelope]
              }
            } else {
              List(OpenWebStandardPayloadEnvelope(message.id, message.clientId, device.deviceId, pnInfo.appName, message.contextId.orEmpty,
                token, OpenWebStandardPayload(Array.empty), headers.toMap, message.meta))
            }
          } else {
            PNCallbackEvent(message.id, message.clientId, device.deviceId, InternalStatus.InvalidToken, MobilePlatform.OPENWEB, device.appName, message.contextId.orEmpty, "OpenWeb without or empty/invalid token not allowed").persist
            List.empty[OpenWebStandardPayloadEnvelope]
          }
        })
      } else {
        ConnektLogger(LogFile.PROCESSORS).warn(s"OpenWebChannelFormatter dropping ttl-expired message: ${message.id}")
        devicesInfo.map(d => PNCallbackEvent(message.id, message.clientId, d.deviceId, InternalStatus.TTLExpired, MobilePlatform.OPENWEB, d.appName, message.contextId.orEmpty)).persist
        List.empty[OpenWebStandardPayloadEnvelope]
      }
    }
    catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"OpenWebChannelFormatter error for ${message.id}", e)
        throw new ConnektPNStageException(message.id, message.clientId, message.deviceId, InternalStatus.StageError, message.appName, message.platform, message.contextId.orEmpty, message.meta, "OpenWebChannelFormatter::".concat(e.getMessage), e)
    }
  }

}
