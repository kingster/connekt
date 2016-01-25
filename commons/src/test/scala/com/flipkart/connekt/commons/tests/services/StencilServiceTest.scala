package com.flipkart.connekt.commons.tests.services

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.{Stencil, StencilEngine}
import com.flipkart.connekt.commons.iomodels.EmailRequestData
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.tests.BaseCommonsTest
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._

/**
 *
 *
 * @author durga.s
 * @version 12/16/15
 */
class StencilServiceTest extends BaseCommonsTest {

  val stencil = new Stencil
  stencil.id = StringUtils.generateRandomStr(10)
  stencil.engine = StencilEngine.VELOCITY
  stencil.engineFabric = """{
                           |	"cType": "EMAIL",
                           |	"subjectVtl": "Order for $product, $booleanValue, $integerValue",
                           |	"bodyHtmlVtl": "Hello $name, Price for $product is $price"
                           |}""".stripMargin
  stencil.createdBy = "unitTest"
  stencil.updatedBy = "unitTest"
  stencil.version = 1

  val product = StringUtils.generateRandomStr(10)
  val name = StringUtils.generateRandomStr(10)
  val price = StringUtils.generateRandomStr(10)
  val payload =
    s"""{
       |"product" : "$product",
       |"name" : "$name",
       |"price" : "$price",
       |"booleanValue" : true,
       |"integerValue": 1678
       |}
    """.stripMargin

  val subjectResult = s"Order for $product, true, 1678"
  val bodyHtmlResult = s"Hello $name, Price for $product is $price"


  "Stencil Service" should "add stencil" in {
    noException should be thrownBy StencilService.add(stencil)
  }

  "Stencil Service" should "get the stencil" in {
    StencilService.get(stencil.id).get.toString shouldEqual stencil.toString
  }

  "Stencil Service" should "render the stencil for given ConnektRequest" in {
    StencilService.render(Some(stencil), payload.getObj[ObjectNode]).get shouldEqual EmailRequestData(subjectResult, bodyHtmlResult)
  }
}
