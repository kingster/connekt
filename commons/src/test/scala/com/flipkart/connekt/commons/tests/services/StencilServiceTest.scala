package com.flipkart.connekt.commons.tests.services

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.iomodels.{PNRequestData, PNRequestInfo, ConnektRequest}
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.tests.BaseCommonsTest
import com.flipkart.connekt.commons.utils.StringUtils._
/**
 *
 *
 * @author durga.s
 * @version 12/16/15
 */
class StencilServiceTest extends BaseCommonsTest {

  "Stencil Service apply" should "render the stencil for given ConnektRequest" in {
    val pnData: ObjectNode =
      """
        |{
        |	"message": "_phantomastray_",
        |	"title": "Do not go gentle into that good night.",
        |	"id": "pqwx2p2x321122228w2t1wxt",
        |	"triggerSound": true,
        |	"notificationType": "Text"
        |}
      """.stripMargin.getObj[ObjectNode]

    val w = md5("Stencil")

    val ckRequest = ConnektRequest(
      "489f8f1e-d88d-44be-809d-44c3dadb0a9c", "PN", "H", "cktSampleApp-stn0x1", 1231231, 324324,
      PNRequestInfo("android", "ConnectSampleApp", "d7ae09474408d039ecad4534ed040f4a", ackRequired = false, delayWhileIdle = false),
      PNRequestData(pnData),
      Map()
    )

    val renderedPNRequest = StencilService.render(ckRequest)
    assert(renderedPNRequest.isDefined)
  }
}
