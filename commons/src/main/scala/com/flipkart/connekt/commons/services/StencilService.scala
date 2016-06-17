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
import com.flipkart.connekt.commons.entities.{Bucket, Stencil, StencilComponents, StencilEngine}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.sync.SyncType._
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncMessage, SyncType}
import com.flipkart.metrics.Timed

import scala.util.{Failure, Success, Try}

object StencilService extends Instrumented with SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.STENCIL_CHANGE, SyncType.STENCIL_BUCKET_CHANGE, SyncType.STENCIL_COMPONENTS_UPDATE, SyncType.STENCIL_FABRIC_CHANGE))
  private val stencilDao = DaoFactory.getStencilDao

  private def cacheKey(id: String, version: Option[String] = None) = id + version.getOrElse("")
  def fabricKey(id: String, component: String) = id + component

  def checkStencil(stencil: Stencil): Try[Boolean] = {
    try {
      val fabric = stencil.engine match {
        case StencilEngine.GROOVY =>
          FabricMaker.create[GroovyFabric](stencil.engineFabric)
        case StencilEngine.VELOCITY =>
          FabricMaker.createVtlFabric(stencil.engineFabric)
      }
      Success(true)
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }

  @Timed("render")
  def render(stencil: Stencil, req: ObjectNode): AnyRef = {
    LocalCacheManager.getCache(LocalCacheType.EngineFabrics).get[EngineFabric](cacheKey(fabricKey(stencil.id, stencil.component), Option(stencil.version.toString))).orElse {
      val fabric = stencil.engine match {
        case StencilEngine.GROOVY =>
          FabricMaker.create[GroovyFabric](stencil.engineFabric)
        case StencilEngine.VELOCITY =>
          FabricMaker.createVtlFabric(stencil.engineFabric)
      }
      LocalCacheManager.getCache(LocalCacheType.EngineFabrics).put[EngineFabric](cacheKey(fabricKey(stencil.id, stencil.component), Option(stencil.version.toString)), fabric)
      Option(fabric)
    }.map(_.compute(stencil.id, req)).orNull
  }

  @Timed("add")
  def add(id: String, stencils: List[Stencil]): Try[Unit] = {
    stencils.foreach(stencil => {
      stencilDao.writeStencil(stencil)
    })
    LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id), stencils)
    Success(Unit)
  }

  @Timed("update")
  def update(id: String, stencils: List[Stencil]): Try[Unit] = {
    stencils.foreach(stencil => {
      stencilDao.writeStencil(stencil)
    })
    SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_CHANGE, List(id)))
    LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id), stencils)
    Success(Unit)
  }

  @Timed("update")
  def updateWithIdentity(id: String, prevName: String, stencils: List[Stencil]): Try[Unit] = {
    stencils.foreach(stencil => {
      stencilDao.updateStencilWithIdentity(prevName, stencil)
    })
    SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_CHANGE, List(id)))
    LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id), stencils)
    Success(Unit)
  }

  @Timed("get")
  def get(id: String, version: Option[String] = None) = {
    LocalCacheManager.getCache(LocalCacheType.Stencils).get[List[Stencil]](cacheKey(id, version)).orElse {
      val stencils = stencilDao.getStencils(id, version)
      LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(id, version), stencils)
      Option(stencils)
    }
  }

  @Timed("get")
  def getStencilByName(name: String, version: Option[String] = None) = {
    LocalCacheManager.getCache(LocalCacheType.Stencils).get[List[Stencil]](cacheKey(name, version)).orElse {
      val stencils = stencilDao.getStencilByName(name, version)
      LocalCacheManager.getCache(LocalCacheType.Stencils).put[List[Stencil]](cacheKey(name, version), stencils)
      Option(stencils)
    }
  }

  @Timed("getBucket")
  def getBucket(name: String): Option[Bucket] = {
    LocalCacheManager.getCache(LocalCacheType.StencilsBucket).get[Bucket](name).orElse {
      val bucket = stencilDao.getBucket(name)
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
        Success(stencilDao.writeBucket(bucket))
    }
  }

  @Timed("getStencilComponents")
  def getStencilComponents(id: String): Option[StencilComponents] = {
    LocalCacheManager.getCache(LocalCacheType.StencilComponents).get[StencilComponents](id).orElse {
      val stencilComponents = stencilDao.getStencilComponents(id)
      stencilComponents.foreach(b => LocalCacheManager.getCache(LocalCacheType.StencilComponents).put[StencilComponents](id, b))
      stencilComponents
    }
  }

  @Timed("addstencilComponents")
  def addStencilComponents(stencilComponents: StencilComponents): Try[Unit] = {
    stencilDao.writeStencilComponents(stencilComponents)
    LocalCacheManager.getCache(LocalCacheType.StencilComponents).put[StencilComponents](stencilComponents.sType, stencilComponents)
    Success(Unit)
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Unit = {
    _type match {
      case SyncType.STENCIL_CHANGE => Try_ {
        LocalCacheManager.getCache(LocalCacheType.Stencils).remove(cacheKey(args.head.toString))
        LocalCacheManager.getCache(LocalCacheType.Stencils).remove(cacheKey(args.head.toString, args.lastOption.map(_.toString)))
      }
      case SyncType.STENCIL_FABRIC_CHANGE => Try_ {
        LocalCacheManager.getCache(LocalCacheType.EngineFabrics).remove(cacheKey(args.head.toString, args.lastOption.map(_.toString)))
      }
      case SyncType.STENCIL_BUCKET_CHANGE => Try_ {
        LocalCacheManager.getCache(LocalCacheType.StencilsBucket).remove(args.head.toString)
      }
      case SyncType.STENCIL_COMPONENTS_UPDATE => Try_ {
        LocalCacheManager.getCache(LocalCacheType.StencilComponents).remove(args.head.toString)
      }
      case _ =>
    }
  }
}
