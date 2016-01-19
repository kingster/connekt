package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{Stencil, StencilEngine}
import com.flipkart.connekt.commons.entities.fabric.{FabricMaker, GroovyFabric, PNGroovyFabric}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest, PNRequestData}
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 * Created by kinshuk.bairagi on 14/12/15.
 */
object StencilService {


  def render(req:ConnektRequest):Option[ChannelRequestData] = {
    val stencil = DaoFactory.getStencilDao.getStencil(req.templateId)
    stencil.map(st =>
      st.engine match {
        case StencilEngine.GROOVY =>
          val fabric: GroovyFabric = FabricMaker.create(st.id, st.engineFabric)
          req.channel match {
            case "PN" =>
              val morphedPNRequestData = req.channelData.asInstanceOf[PNRequestData].copy(data = fabric.asInstanceOf[PNGroovyFabric].getData(req.id, req.channelData.asInstanceOf[PNRequestData].data))
              ConnektLogger(LogFile.SERVICE).debug(s"Rendered pnRequestData: ${morphedPNRequestData.getJson}")
              morphedPNRequestData
          }
        case StencilEngine.VELOCITY =>
          req.channelData
      }
    )
  }

  def add(stencil: Stencil) = DaoFactory.getStencilDao.updateStencil(stencil)

  def update(stencil: Stencil) = DaoFactory.getStencilDao.updateStencil(stencil)

  def get(id: String) = DaoFactory.getStencilDao.getStencil(id)



}

