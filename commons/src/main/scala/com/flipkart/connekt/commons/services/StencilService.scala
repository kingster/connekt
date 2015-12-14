package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.StencilEngine
import com.flipkart.connekt.commons.entities.fabric.{FabricMaker, GroovyFabric, PNGroovyFabric}
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest, PNRequestData}

/**
 * Created by kinshuk.bairagi on 14/12/15.
 */
object StencilService {

  def apply(req:ConnektRequest):Option[ChannelRequestData] = {
    val stencil = DaoFactory.getStencilDao.getStencil(req.templateId)
    stencil.map(st =>
      st.engine match {
        case StencilEngine.GROOVY =>
          val fabric: GroovyFabric = FabricMaker.create(st.engineFabric, "_groovyClassName_")
          req.channel match {
            case "PN" =>
              val morphedPNRequestData = req.channelData.asInstanceOf[PNRequestData].copy(data = fabric.asInstanceOf[PNGroovyFabric].getData(req.id, req.channelData.asInstanceOf[PNRequestData].data))
              morphedPNRequestData
          }
        case StencilEngine.VELOCITY =>
          req.channelData
      }
   )
  }

}

