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

import java.util.concurrent._
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import akka.Done
import com.flipkart.connekt.commons.cache.{DistributedCacheManager, DistributedCacheType}
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.metrics.Timed
import com.roundeights.hasher.Implicits._

import scala.collection.JavaConverters._
import scala.concurrent.Promise
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}
object DeviceDetailsService extends Instrumented {

  lazy val dao = DaoFactory.getDeviceDetailsDao

  def bootstrap() = dao.get("ConnectSampleApp", StringUtils.generateRandomStr(15))

  @Timed("add")
  def add(deviceDetails: DeviceDetails): Try[Boolean] = Try_#(message = "DeviceDetailsService.add Failed") {
    dao.add(deviceDetails.appName, deviceDetails)
    DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](cacheKey(deviceDetails.appName, deviceDetails.deviceId), deviceDetails)
    if (deviceDetails.userId != null)
      DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).remove(cacheKey(deviceDetails.appName, deviceDetails.userId))
    ServiceFactory.getCallbackService.enqueueCallbackEvents(List(deviceDetails.toCallbackEvent)).get
    BigfootService.ingest(deviceDetails.toBigfootFormat).get
  }

  /**
   *
   * @param deviceId: device Id
   * @param deviceDetails: device Details
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
          ServiceFactory.getCallbackService.enqueueCallbackEvents(List(deviceDetails.toCallbackEvent)).get
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
   * @param appName: app Name
   * @param deviceId: device id
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
          ServiceFactory.getCallbackService.enqueueCallbackEvents(List(device.copy(active = false).toCallbackEvent)).get
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

  /**
   * getAll Devices for given AppName.
   * @param appName (CaseSensitive)
   * @return
   */
  def getAll(appName: String): Try[Iterator[DeviceDetails]] = Try_#(message = "DeviceDetailsService.getAll Failed") {
    dao.getAll(appName)
  }

  private val warmupThreadCounter = new AtomicInteger(0)

  private val pool = new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue[Runnable](25), new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val thread = new Thread(r)
      thread.setName("WarmUp_Thread_" + warmupThreadCounter.incrementAndGet())
      thread
    }
  })

  sealed case class WarmUpStatus(currentCount: AtomicLong, status: Promise[Done])
  sealed case class WarmUpResult(status: String, currentCount: Long, debug: Option[String])

  private val warmUpTasks = new ConcurrentHashMap[String, WarmUpStatus]().asScala

  def cacheWarmUp(appName: String): String = {
    val jobId = appName + "_" + StringUtils.generateRandomStr(5)
    warmUpTasks.put(jobId, WarmUpStatus(new AtomicLong(0), Promise[Done]()))

    val task = new Runnable {
      override def run() = {
        try {
          getAll(appName).get
            .grouped(5000)
            .foreach(chunk => {
            pool.execute(new Runnable {
              override def run() = {
                try {
                  DistributedCacheManager.getCache(DistributedCacheType.DeviceDetails).put[DeviceDetails](chunk.toList.map(d => (cacheKey(appName, d.deviceId), d)))
                  warmUpTasks(jobId).currentCount.getAndAdd(chunk.size.toLong)
                }
                catch {
                  case e: Exception =>
                    ConnektLogger(LogFile.SERVICE).error(s"Cache warm-up failure for app: $appName jobId: $jobId", e)
                    warmUpTasks(jobId).status.failure(e)
                }
              }
            })
          })

          warmUpTasks(jobId).status.success(Done)
          ConnektLogger(LogFile.SERVICE).error(s"Cache warm-up successful for app: $appName jobId: $jobId")
        } catch {
          case e: Exception =>
            ConnektLogger(LogFile.SERVICE).error(s"Cache warm-up failure for app: $appName jobId: $jobId", e)
            warmUpTasks(jobId).status.failure(e)
        }
      }
    }

    new Thread(task).start()
    jobId
  }

  def cacheWarmUpJobStatus(jobId: String): WarmUpResult = {
    warmUpTasks.get(jobId) match {
      case None => WarmUpResult("INVALID_JOB", -1, None)
      case Some(entry) =>
        val currentCount = entry.currentCount.longValue()
        entry.status.future.value match {
          case None => WarmUpResult("RUNNING", currentCount, None)
          case Some(Success(Done)) => WarmUpResult("COMPLETED", currentCount, None)
          case Some(Failure(t)) => WarmUpResult("FAILED", currentCount, Option(t.getMessage))
        }
    }
  }

  def cacheJobStatus = warmUpTasks
}
