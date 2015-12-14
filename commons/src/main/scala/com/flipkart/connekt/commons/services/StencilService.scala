package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.StencilEngine
import com.flipkart.connekt.commons.entities.fabric.PNGroovyFabric
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest, PNRequestData}
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 * Created by kinshuk.bairagi on 14/12/15.
 */
object StencilService {

  def apply(req:ConnektRequest):Option[ChannelRequestData] = {
    val stensil = DaoFactory.getStencilDao.getStencil(req.templateId)
    stensil.map(st =>
      st.engine match {
        case StencilEngine.GROOVY =>
          val fabric = st.engineFabric.getObj[PNGroovyFabric]
          fabric.fabricate[PNGroovyFabric](fabric.pnGroovy,st.id ).getData(req.channelData.asInstanceOf[PNRequestData].data).asInstanceOf[PNRequestData]
        case StencilEngine.VELOCITY =>
          req.channelData
      }
   )
  }

}

