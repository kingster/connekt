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
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._

class MessageServiceTest extends CommonsBaseTest {
  val data = "{        \"message\": \"Hello World\",        \"title\": \"Hello world\",        \"id\": \"pqwx2p2x321122228w2t1wxt\",        \"triggerSound\" : true,        \"notificationType\" : \"Text\"}"
  val pnRequestInfo = PNRequestInfo("android", "connekt", Set[String]("bbd505411b210e38b15142bd6a0de0f6"), None, true, true)
  val pnRequestData = PNRequestData(data.getObj[ObjectNode])
  val request = ConnektRequest(null, contextId = None, "PN", "H", Option("retail-app-base-0x23"), Option(1231231), Option(324324), pnRequestInfo, pnRequestData, StringUtils.getObjectNode, Map())
  var id: String = null

  /*
    "IMessage Service Test" should "persist/get/update request" in {
      val messageService = ServiceFactory.getMessageService
      val persistResult = messageService.persistRequest(request, "fk-connekt-pn", isCrucial = true)
      persistResult.isSuccess shouldEqual true
      id = persistResult.get
      messageService.updateRequestStatus(id, pnSendStatus).isSuccess shouldEqual true

    }

    "IMessage Service Test" should "get request" in {
      val messageService = ServiceFactory.getMessageService
      messageService.getRequestInfo(id).isSuccess shouldEqual true
    }

    "IMessage Service Test" should "update request" in {
      val messageService = ServiceFactory.getMessageService
      messageService.updateRequestStatus(id, pnSendStatus).isSuccess shouldEqual true
    }
  */

}
