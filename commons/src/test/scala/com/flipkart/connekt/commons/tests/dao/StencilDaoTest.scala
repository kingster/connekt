package com.flipkart.connekt.commons.tests.dao

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.tests.BaseCommonsTest

/**
 *
 *
 * @author durga.s
 * @version 12/16/15
 */
class StencilDaoTest extends BaseCommonsTest {

  "Fetch Stencil" should "return a stencil" in {
    val stencil = DaoFactory.getStencilDao.getStencil("cktSampleApp-stn0x1")
    
    assert(stencil.isDefined)
  }
}
