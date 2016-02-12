package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.cache.{DistributedCacheType, DistributedCacheManager}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.metrics.Timed

import scala.util.{Try, Failure, Success}

/**
 * Created by kinshuk.bairagi on 16/01/16.
 */
object DeviceDetailsService extends Instrumented{

  lazy val dao = DaoFactory.getDeviceDetailsDao

  @Timed("add")
  def add(deviceDetails: DeviceDetails): Try[Unit] = {
    try {
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))
      dao.add(deviceDetails.appName, deviceDetails)
      BigfootService.ingest(deviceDetails.toBigfootEntity)
      Success(Unit)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Device Detail add service failed, e: ${e.getMessage}")
        Failure(e)
    }
  }

  /**
   *
   * @param deviceId
   * @param deviceDetails
   */
  @Timed("update")
  def update(deviceId: String, deviceDetails: DeviceDetails): Try[Unit] = {
    get(deviceDetails.appName, deviceId) match {
      case Success(device) if device.isDefined =>
        try {
          val d = device.get
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(d.appName, d.userId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(d.appName, d.token))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(d.appName, d.deviceId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))

          dao.update(deviceDetails.appName, deviceId, deviceDetails)
          BigfootService.ingest(deviceDetails.toBigfootEntity)
          Success(Unit)
        } catch {
          case e: Exception => Failure(e)
        }

      case Success(device) if device.isEmpty => Failure(new Throwable(s"No Device Detail found for id: [$deviceId] to update."))
      case Failure(e) => Failure(e)
    }
  }

  /*
      get and delete device if device exists
      And if device exists, delete corresponding cache entry
      and mark device as INACTIVE in bigfoot
   */
  @Timed("delete")
  def delete(appName: String, deviceId: String): Try[Unit] = {
    get(appName, deviceId) match {
      case Success(device) if device.isDefined =>
        val d = device.get
        try {
          dao.delete(appName, deviceId)
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(d.appName, d.userId))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(d.appName, d.token))
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(d.appName, d.deviceId))
          BigfootService.ingest(d.copy(active = false).toBigfootEntity)
          Success(Unit)
        } catch {
          case e: Exception =>
            Failure(e)
        }
      case Success(device) if device.isEmpty =>
        Failure(new Throwable(s"No Device Detail found for app: [$appName] id: [$deviceId] to delete."))
      case Failure(e) =>
        Failure(e)
    }
  }


  @Timed("getByTokenId")
  def getByTokenId(appName: String, tokenId: String): Try[Option[DeviceDetails]] = {
    try {
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](cacheKey(appName, tokenId)) match {
        case Some(device) => Try(Some(device))
        case None =>
          val device = dao.getByTokenId(appName, tokenId)
          device.foreach(d => DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(appName, tokenId), d))
          Success(device)
      }
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Device Detail getByToken service failed, e: ${e.getMessage}", e)
        Failure(e)
    }
  }


  @Timed("getByUserId")
  def getByUserId(appName: String, userId: String): Try[List[DeviceDetails]] = {
    try {
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[List[DeviceDetails]](cacheKey(appName, userId)) match {
        case Some(deviceList) => Success(deviceList)
        case None =>
          val deviceList = dao.getByUserId(appName, userId)
          DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[List[DeviceDetails]](cacheKey(appName, userId), deviceList)
          Success(deviceList)
      }
    }
    catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Device Detail get by userId service failed, e: ${e.getMessage}", e)
        Failure(e)
    }
  }

  //TODO : Change with underlying mget implementation
  //Per our usage changing it to return only a list of deviceDetails found, since, biz wants us to send
  //it to as many devices we find.
  @Timed("mget")
  def get(appName: String, deviceIds: List[String]): List[DeviceDetails] = {
    deviceIds.map(get(appName,_).getOrElse(None)).flatten
  }

  @Timed("get")
  def get(appName: String, deviceId: String): Try[Option[DeviceDetails]] = {
    try {
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).get[DeviceDetails](cacheKey(appName, deviceId)) match {
        case Some(device) => Success(Some(device))
        case None =>
          val device = dao.get(appName, deviceId)
          device match {
            case None =>
            case Some(x) =>
              DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(appName, deviceId), x)
          }
          Success(device)
      }
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error(s"Device Detail get service failed, e: ${e.getMessage}", e)
        Failure(e)
    }
  }

  private def cacheKey(appName: String, id: String): String = appName + "_" + id

}
