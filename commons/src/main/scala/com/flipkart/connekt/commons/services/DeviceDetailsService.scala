package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.cache.{DistributedCacheType, DistributedCacheManager}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.metrics.Timed

import scala.util.{Try, Failure, Success}

import com.flipkart.connekt.commons.core.Handlers._

/**
 * Created by kinshuk.bairagi on 16/01/16.
 */
object DeviceDetailsService extends Instrumented {

  lazy val dao = DaoFactory.getDeviceDetailsDao

  @Timed("add")
  def add(deviceDetails: DeviceDetails): Try[Unit] = Try_(message = "Device Detail add service failed") {
    dao.add(deviceDetails.appName, deviceDetails)
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))
    BigfootService.ingest(deviceDetails.toBigfootEntity)
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
        Try__ {
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.userId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.token))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.deviceId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))

          dao.update(deviceDetails.appName, deviceId, deviceDetails)
          BigfootService.ingest(deviceDetails.toBigfootEntity)
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
        Try__ {
          dao.delete(appName, deviceId)
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.userId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.token))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(device.appName, device.deviceId))
          BigfootService.ingest(device.copy(active = false).toBigfootEntity)
        }
      case None =>
        Failure(new Throwable(s"No Device Detail found for app: [$appName] id: [$deviceId] to delete."))
    }
  }


  @Timed("getByTokenId")
  def getByTokenId(appName: String, tokenId: String): Try[Option[DeviceDetails]] = Try_(message = "Device Detail getByToken service failed") {
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](cacheKey(appName, tokenId)).orElse {
      val device = dao.getByTokenId(appName, tokenId)
      device.foreach(d => DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(appName, tokenId), d))
      device
    }
  }


  @Timed("getByUserId")
  def getByUserId(appName: String, userId: String): Try[List[DeviceDetails]] = Try_(message = "DeviceDetail userId service failed") {
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[List[DeviceDetails]](cacheKey(appName, userId)).getOrElse {
      val deviceList = dao.getByUserId(appName, userId)
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[List[DeviceDetails]](cacheKey(appName, userId), deviceList)
      deviceList
    }
  }

  //TODO : Change with underlying mget implementation
  //Per our usage changing it to return only a list of deviceDetails found, since, biz wants us to send
  //it to as many devices we find.
  @Timed("mget")
  def get(appName: String, deviceIds: List[String]): List[DeviceDetails] = {
    deviceIds.flatMap(get(appName, _).getOrElse(None))
  }

  @Timed("get")
  def get(appName: String, deviceId: String): Try[Option[DeviceDetails]] = Try_(message = "Device Detail get service failed") {
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](cacheKey(appName, deviceId)).orElse {
      val device = dao.get(appName, deviceId)
      device.foreach(d => DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(appName, deviceId), d))
      device
    }
  }

  private def cacheKey(appName: String, id: String): String = appName + "_" + id

}
