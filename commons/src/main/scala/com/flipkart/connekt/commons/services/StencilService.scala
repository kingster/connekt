package com.flipkart.connekt.commons.services

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.fabric._
import com.flipkart.connekt.commons.entities.{Stencil, StencilEngine}
import com.flipkart.connekt.commons.iomodels.ChannelRequestData

import scala.util.{Failure, Success, Try}

/**
 * Created by kinshuk.bairagi on 14/12/15.
 */
object StencilService {

  def render(stencil: Option[Stencil], req: ObjectNode): Option[ChannelRequestData] = {
    stencil.map(st =>
      st.engine match {
        case StencilEngine.GROOVY =>
          val fabric: GroovyFabric = FabricMaker.create(st.id, st.engineFabric)
          fabric.renderData(st.id, req)
        case StencilEngine.VELOCITY =>
          val fabric = FabricMaker.createVtlFabric(st.id, st.engineFabric)
          fabric.renderData(st.id, req)
      }
    )
  }

  def add(stencil: Stencil): Try[Unit] = {
    try {
      val fabric = stencil.engine match {
        case StencilEngine.GROOVY =>
          FabricMaker.create(stencil.id, stencil.engineFabric).asInstanceOf[GroovyFabric]
        case StencilEngine.VELOCITY =>
          val fabric = FabricMaker.createVtlFabric(stencil.id, stencil.engineFabric)
      }
      DaoFactory.getStencilDao.updateStencil(stencil)
      Success(Nil)
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }

  def get(id: String) = DaoFactory.getStencilDao.getStencil(id)
}

