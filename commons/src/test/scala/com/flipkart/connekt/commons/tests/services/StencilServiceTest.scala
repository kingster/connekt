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
package com.flipkart.connekt.commons.tests.services

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.{Bucket, Stencil, StencilEngine}
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._

class StencilServiceTest extends CommonsBaseTest {

  val stencil = new Stencil
  stencil.id = StringUtils.generateRandomStr(10)
  stencil.name = StringUtils.generateRandomStr(10)
  stencil.component = "data"
  stencil.engine = StencilEngine.VELOCITY
  stencil.engineFabric = """{
                           |	"data": "Order for $product, $booleanValue, $integerValue"
                           |}""".stripMargin
  stencil.createdBy = "connekt-genesis"
  stencil.updatedBy = "connekt-genesis"
  stencil.version = 1
  stencil.bucket = "GLOBAL"

  val product = StringUtils.generateRandomStr(10)
  val name = StringUtils.generateRandomStr(10)
  val price = StringUtils.generateRandomStr(10)
  val payload =
    s"""{
       |"product" : "$product",
       |"booleanValue" : true,
       |"integerValue": 1678
       |}
    """.stripMargin

  //  val dataResult = s"Order for $product, true, 1678"
  val dataResult = s"""{
                      |	"data": "Order for $product, true, 1678"
                      |}""".stripMargin

  val bucket = new Bucket
  bucket.id = StringUtils.generateRandomStr(10)
  bucket.name = StringUtils.generateRandomStr(10)


  "Stencil Service" should "add stencil" in {
    noException should be thrownBy StencilService.add(stencil.id, List(stencil))
  }

  "Stencil Service" should "get the stencil" in {
    StencilService.get(stencil.id).get.head.toString shouldEqual stencil.toString
  }

  "Stencil Service" should "render the stencil for given ConnektRequest" in {
    StencilService.render(stencil, payload.getObj[ObjectNode])  shouldEqual dataResult
  }

    "Stencil Service" should "add bucket" in {
      noException should be thrownBy StencilService.addBucket(bucket)
    }

    "Stencil Service" should "get bucket" in {
      StencilService.getBucket(bucket.name).get.toString shouldEqual bucket.toString
    }
}
