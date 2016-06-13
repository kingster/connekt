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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.fabric._
import com.flipkart.connekt.commons.entities.{Bucket, Stencil, StencilEngine}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.sync.SyncType._
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncMessage, SyncType}
import com.flipkart.metrics.Timed

import scala.util.{Failure, Success, Try}

object StencilService extends Instrumented with SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.STENCIL_CHANGE, SyncType.STENCIL_BUCKET_CHANGE))

  private def cacheKey(id: String, version: Option[String] = None) = id + version.getOrElse("")

  private def fabricKey(id: String, tag: String) = id + tag

  def checkStencil(stencil: Stencil): Unit = {
    try {
      val fabric = stencil.engine match {
        case StencilEngine.GROOVY =>
          FabricMaker.create[GroovyFabric](stencil.id, stencil.engineFabric)
        case StencilEngine.VELOCITY =>
          FabricMaker.createVtlFabric(stencil.id, stencil.engineFabric)
      }
    } catch {
      case e: Exception =>
        throw e
    }
  }

  @Timed("render")
  def render(stencil: Stencil, req: ObjectNode): String = {
    LocalCacheManager.getCache(LocalCacheType.EngineFabrics).get[EngineFabric](cacheKey(fabricKey(stencil.id, stencil.tag), Option(stencil.version.toString))).orElse {
      val fabric = stencil.engine match {
        case StencilEngine.GROOVY =>
          FabricMaker.create[GroovyFabric](stencil.id, stencil.engineFabric)
        case StencilEngine.VELOCITY =>
          FabricMaker.createVtlFabric(stencil.id, stencil.engineFabric)
      }
      LocalCacheManager.getCache(LocalCacheType.EngineFabrics).put[EngineFabric](cacheKey(fabricKey(stencil.id, stencil.tag), Option(stencil.version.toString)), fabric)
      Option(fabric)
    }.map(_.renderData(stencil.id, req)).orNull
  }

  @Timed("add")
  def add(id: String, stencils: List[Stencil]): Try[Unit] = {
    stencils.foreach(stencil => {
      DaoFactory.getStencilDao.writeStencil(stencil)
    })
    LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id), stencils)
    Success()
  }

  @Timed("update")
  def update(id: String, stencils: List[Stencil]): Try[Unit] = {
    stencils.foreach(stencil => {
      DaoFactory.getStencilDao.writeStencil(stencil)
    })
    SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_CHANGE, List(id)))
    LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id), stencils)
    Success()
  }

  @Timed("update")
  def updateWithIdentity(id: String, prevName : String, stencils: List[Stencil]): Try[Unit] = {
    stencils.foreach(stencil => {
      DaoFactory.getStencilDao.updateStencilWithIdentity(prevName , stencil)
    })
    SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_CHANGE, List(id)))
    LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id), stencils)
    Success()
  }

  @Timed("get")
  def get(id: String, version: Option[String] = None) = {
    LocalCacheManager.getCache(LocalCacheType.Stencils).get[List[Stencil]](cacheKey(id, version)).orElse {
      val stencils = DaoFactory.getStencilDao.getStencil(id, version)
      LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id, version), stencils)
      Option(stencils)
    }
  }

  @Timed("getBucket")
  def getBucket(name: String): Option[Bucket] = {
    LocalCacheManager.getCache(LocalCacheType.StencilsBucket).get[Bucket](name).orElse {
      val bucket = DaoFactory.getStencilDao.getBucket(name)
      bucket.foreach(b => LocalCacheManager.getCache(LocalCacheType.StencilsBucket).put[Bucket](name, b))
      bucket
    }
  }

  @Timed("addBucket")
  def addBucket(bucket: Bucket): Try[Unit] = {
    getBucket(bucket.name) match {
      case Some(bck) =>
        Failure(new Exception(s"Bucket already exist for name: ${bucket.name}"))
      case _ =>
        LocalCacheManager.getCache(LocalCacheType.StencilsBucket).put[Bucket](bucket.name, bucket)
        Success(DaoFactory.getStencilDao.writeBucket(bucket))
    }
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Unit = {
    _type match {
      case SyncType.STENCIL_CHANGE => Try_ {
        LocalCacheManager.getCache(LocalCacheType.Stencils).remove(cacheKey(args.head.toString))
        LocalCacheManager.getCache(LocalCacheType.Stencils).remove(cacheKey(args.head.toString, args.lastOption.map(_.toString)))
        LocalCacheManager.getCache(LocalCacheType.EngineFabrics).remove(cacheKey(args.head.toString, args.lastOption.map(_.toString)))
      }
      case SyncType.STENCIL_BUCKET_CHANGE => Try_ {
        LocalCacheManager.getCache(LocalCacheType.StencilsBucket).remove(args.head.toString)
      }
      case _ =>
    }
  }
}
