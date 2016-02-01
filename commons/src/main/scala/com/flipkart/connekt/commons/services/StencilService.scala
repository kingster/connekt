package com.flipkart.connekt.commons.services

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.fabric._
import com.flipkart.connekt.commons.entities.{Bucket, Stencil, StencilEngine}
import com.flipkart.connekt.commons.iomodels.ChannelRequestData

import scala.util.{Failure, Success, Try}

/**
 * Created by kinshuk.bairagi on 14/12/15.
 */
object StencilService {

  private def checkStencil(stencil: Stencil): Try[Boolean] = {
    try {
      val fabric = stencil.engine match {
        case StencilEngine.GROOVY =>
          FabricMaker.create(stencil.id, stencil.engineFabric).asInstanceOf[GroovyFabric]
        case StencilEngine.VELOCITY =>
          val fabric = FabricMaker.createVtlFabric(stencil.id, stencil.engineFabric)
      }
      Success(true)
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }

  def render(stencil: Stencil, req: ObjectNode): Option[ChannelRequestData] = {
      val fabric = stencil.engine match {
        case StencilEngine.GROOVY =>
          FabricMaker.create[GroovyFabric](stencil.id, stencil.engineFabric)
        case StencilEngine.VELOCITY =>
          FabricMaker.createVtlFabric(stencil.id, stencil.engineFabric)
      }
      Some(fabric.renderData(stencil.id, req))
  }

  def add(stencil: Stencil): Try[Unit] = {
    get(stencil.id) match {
      case Some(stn) =>
        Failure(throw new Exception(s"stencil already exist for id: ${stencil.id}"))
      case _ =>
        checkStencil(stencil) match {
          case Success(b) =>
            DaoFactory.getStencilDao.writeStencil(stencil)
            Success()
          case Failure(e) =>
            Failure(e)
        }
    }
  }

  def update(stencil: Stencil): Try[Unit] = {
    get(stencil.id) match {
      case Some(stn) =>
        checkStencil(stn) match {
          case Success(b) =>
            DaoFactory.getStencilDao.writeStencil(stn)
            Success()
          case Failure(e) =>
            Failure(e)
        }
      case _ =>
        Failure(throw new Exception(s"No stencil for id ${stencil.id}"))
    }
  }

  def get(id: String, version: Option[String] = None) = DaoFactory.getStencilDao.getStencil(id, version)

  def getBucket(name: String): Option[Bucket] = DaoFactory.getStencilDao.getBucket(name)

  def addBucket(bucket: Bucket) : Try[Unit] = {
    getBucket(bucket.name) match {
      case Some(bck) =>
        Failure(throw new Exception(s"Bucket already exist for name: ${bucket.name}"))
      case _ =>
        Success(DaoFactory.getStencilDao.writeBucket(bucket))
    }
  }
}

