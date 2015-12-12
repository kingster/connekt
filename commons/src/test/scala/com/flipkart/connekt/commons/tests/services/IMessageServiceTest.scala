package com.flipkart.connekt.commons.tests.services

import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNRequestData, PNRequestInfo}
import com.flipkart.connekt.commons.tests.BaseCommonsTest

/**
 * @author aman.shrivastava on 10/12/15.
 */
class IMessageServiceTest extends BaseCommonsTest {
  val data = "{        \"message\": \"Hello World\",        \"title\": \"Hello world\",        \"id\": \"pqwx2p2x321122228w2t1wxt\",        \"triggerSound\" : true,        \"notificationType\" : \"Text\"}"
  val pnRequestInfo = PNRequestInfo("android", "connekt", "bbd505411b210e38b15142bd6a0de0f6", true, true)
  val pnRequestData = PNRequestData(data)
  val request = ConnektRequest(null, "PN", "H", "retail-app-base-0x23", 1231231, 324324, pnRequestInfo, pnRequestData, Map())
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
