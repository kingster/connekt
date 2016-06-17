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
package com.flipkart.connekt.commons.tests.dao

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{Bucket, Stencil, StencilEngine}
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils

class StencilDaoTest extends CommonsBaseTest {
  val stencil = new Stencil
  stencil.id = StringUtils.generateRandomStr(10)
  stencil.engine = StencilEngine.VELOCITY
  stencil.engineFabric = """{
                           |	"cType": "EMAIL",
                           |	"subjectVtl": "Order for $product, $booleanValue, $integerValue",
                           |	"bodyHtmlVtl": "Hello $name, Price for $product is $price"
                           |}""".stripMargin
  stencil.createdBy = "connekt-genesis"
  stencil.updatedBy = "connekt-genesis"
  stencil.version = 1
  stencil.bucket = "GLOBAL"
  var bucket: Bucket = new Bucket
  bucket.id = StringUtils.generateRandomStr(10)
  bucket.name = StringUtils.generateRandomStr(10)

  "Stencil Dao write Test" should "not throw exception" in {
    noException should be thrownBy DaoFactory.getStencilDao.writeStencil(stencil)
  }

  "Fetch Stencil" should "return a stencil" in {
    val stencil = DaoFactory.getStencilDao.getStencils("cktSampleApp-stn0x1")
    assert(stencil.nonEmpty)
  }

  "Fetch Stencil by version" should "return a stencil" in {
    val sten = DaoFactory.getStencilDao.getStencils(stencil.id, Option("1"))
    assert(sten.nonEmpty)
  }

  "write bucket" should "not throw exception" in {
    noException should be thrownBy DaoFactory.getStencilDao.writeBucket(bucket)
  }

  "Fetch bucket" should "return bucket " in {
    val buc = DaoFactory.getStencilDao.getBucket(bucket.name)
    assert(buc.isDefined)
  }
}
