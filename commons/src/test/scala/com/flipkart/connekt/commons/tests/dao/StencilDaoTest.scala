package com.flipkart.connekt.commons.tests.dao

import java.util.UUID

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.Stencil
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 *
 *
 * @author durga.s
 * @version 12/16/15
 */
class StencilDaoTest extends CommonsBaseTest {

  val stencilId = UUID.randomUUID().toString

  "Fetch Stencil" should "add a stencil" in {

    noException should be thrownBy DaoFactory.getStencilDao.upsertStencil({
      val stencil = new Stencil()
      stencil.engineFabric = "ehello"
      stencil.id = stencilId
      stencil.createdBy = "ut"
      stencil.updatedBy = "ut"
      stencil
    }).get
  }

  "Fetch Stencil" should "return a stencil" in {
    val stencil = DaoFactory.getStencilDao.getStencil(stencilId)
    
    assert(stencil.isDefined)
  }
}
