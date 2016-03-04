package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.metrics.Timed

import scala.util.{Failure, Try}
import com.roundeights.hasher.Implicits._

/**
 * Created by kinshuk.bairagi on 16/01/16.
 */
object DeviceDetailsService extends Instrumented {

  lazy val dao = DaoFactory.getDeviceDetailsDao

  @Timed("add")
  def add(deviceDetails: DeviceDetails): Try[Unit] = Try_#(message = "DeviceDetailsService.add Failed") {
    dao.add(deviceDetails.appName, deviceDetails)
    if (deviceDetails.userId != null)
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))
    BigfootService.ingest(deviceDetails.toBigfootFormat)
  }

  /**
   *
   * @param deviceId
   * @param deviceDetails
   */
  @Timed("update")
  def update(deviceId: String, deviceDetails: DeviceDetails): Try[Unit] = {
    get(deviceDetails.appName, deviceId) flatMap {
      case Some(device) =>
        Try_ {
          if (device.userId != null)
            DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.userId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.token))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.deviceId))
          if (deviceDetails.userId != null)
            DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))

          dao.update(deviceDetails.appName, deviceId, deviceDetails)
          BigfootService.ingest(deviceDetails.toBigfootFormat)
        }
      case None => Failure(new Throwable(s"No Device Detail found for id: [$deviceId] to update."))
    }

  }


  /*
      get and delete device if device exists
      And if device exists, delete corresponding cache entry
      and mark device as INACTIVE in bigfoot
   */
  @Timed("delete")
  def delete(appName: String, deviceId: String): Try[Unit] = {
    get(appName, deviceId).flatMap {
      case Some(device) =>
        Try_ {
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
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[List[DeviceDetails]](cacheKey(appName, userId)).getOrElse {
      val deviceList = dao.getByUserId(appName, userId)
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[List[DeviceDetails]](cacheKey(appName, userId), deviceList)
      deviceList
    }
  }

  @Timed("mget")
  def get(appName: String, deviceIds: List[String]): Try[List[DeviceDetails]] = Try_#(message = "DeviceDetailsService.mget Failed") {
    val cacheHitsDevices = DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](deviceIds)

    val cacheMissedIds = deviceIds.diff(cacheHitsDevices.keySet.toList)
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

}
