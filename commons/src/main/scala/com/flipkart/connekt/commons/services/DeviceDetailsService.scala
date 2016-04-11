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

import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.metrics.Timed
import com.roundeights.hasher.Implicits._

import scala.reflect.runtime.universe._
import scala.util.{Failure, Try}

object DeviceDetailsService extends Instrumented {

  lazy val dao = DaoFactory.getDeviceDetailsDao

  @Timed("add")
  def add(deviceDetails: DeviceDetails): Try[Boolean] = Try_#(message = "DeviceDetailsService.add Failed") {
    dao.add(deviceDetails.appName, deviceDetails)
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(deviceDetails.appName, deviceDetails.deviceId), deviceDetails)
    if (deviceDetails.userId != null)
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))
    BigfootService.ingest(deviceDetails.toBigfootFormat).get
  }

  /**
   *
   * @param deviceId
   * @param deviceDetails
   */
  @Timed("update")
  def update(deviceId: String, deviceDetails: DeviceDetails): Try[Boolean] = {
    get(deviceDetails.appName, deviceId).flatMap {
      case Some(device) =>
        Try_#(message = "DeviceDetailsService.update Failed") {
          dao.update(deviceDetails.appName, deviceId, deviceDetails)
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(device.appName, device.deviceId), deviceDetails)
          if (device.userId != null)
            DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.userId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.token))
          if (deviceDetails.userId != null)
            DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))
          BigfootService.ingest(deviceDetails.toBigfootFormat).get
        }
      case None =>
        Failure(new Throwable(s"No Device Detail found for id: [$deviceId] to update."))
    }
  }

  /**
   * get and delete device if device exists,
   * if device exists, delete corresponding cache entry
   * and mark device as INACTIVE in bigfoot
   *
   * @param appName
   * @param deviceId
   * @return
   */
  @Timed("delete")
  def delete(appName: String, deviceId: String): Try[Unit] = {
    get(appName, deviceId).flatMap {
      case Some(device) =>
        Try_#(message = "DeviceDetailsService.delete Failed") {
          dao.delete(appName, deviceId)
          if (device.userId != null)
            DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.userId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.token))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.deviceId))
          BigfootService.ingest(device.copy(active = false).toBigfootFormat)
        }
      case None =>
        Failure(new Throwable(s"No Device Detail found for app: [$appName] id: [$deviceId] to delete."))
    }
  }


  @Timed("getByTokenId")
  def getByTokenId(appName: String, tokenId: String): Try[Option[DeviceDetails]] = Try_#(message = "DeviceDetailsService.getByToken Failed") {
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](cacheKey(appName, tokenId)).orElse {
      val device = dao.getByTokenId(appName, tokenId)
      device.foreach(d => DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(appName, tokenId), d))
      device
    }
  }


  @Timed("getByUserId")
  def getByUserId(appName: String, userId: String): Try[List[DeviceDetails]] = Try_#(message = "DeviceDetailsService.getByUserId Failed") {
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[List[DeviceDetails]](cacheKey(appName, userId), typeTag[List[DeviceDetails]]).getOrElse {
      val deviceList = dao.getByUserId(appName, userId)
      if (deviceList.nonEmpty)
        DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[List[DeviceDetails]](cacheKey(appName, userId), deviceList)
      deviceList
    }
  }

  @Timed("mget")
  def get(appName: String, deviceIds: Set[String]): Try[List[DeviceDetails]] = Try_#(message = "DeviceDetailsService.mget Failed") {
    val cacheHitsDevices = DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](deviceIds.map(cacheKey(appName, _)).toList)

    val cacheMissedIds = deviceIds.diff(cacheHitsDevices.values.map(_.deviceId).toSet)
    val cacheMissDevices = dao.get(appName, cacheMissedIds)

    if (cacheMissDevices.nonEmpty)
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheMissDevices.map(device => (cacheKey(appName, device.deviceId), device)))
    cacheMissDevices ++ cacheHitsDevices.values
  }


  @Timed("get")
  def get(appName: String, deviceId: String): Try[Option[DeviceDetails]] = Try_#(message = "DeviceDetailsService.get Failed") {
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](cacheKey(appName, deviceId)).orElse {
      val device = dao.get(appName, deviceId)
      device.foreach(d => DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(appName, deviceId), d))
      device
    }
  }

  private def cacheKey(appName: String, id: String): String = appName.toLowerCase + "_" + id.sha256.hash.hex


  def getAll(appName: String): Try[Iterator[DeviceDetails]] = Try_#(message = "DeviceDetailsService.getAll Failed") {
    dao.getAll(appName)
  }

}
