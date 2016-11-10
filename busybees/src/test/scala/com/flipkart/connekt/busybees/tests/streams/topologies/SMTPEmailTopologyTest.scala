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
package com.flipkart.connekt.busybees.tests.streams.topologies

import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Sink, Source}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{GCMDispatcherPrepare, SMTPDispatcher}
import com.flipkart.connekt.busybees.streams.flows.formaters.{AndroidChannelFormatter, EmailChannelFormatter}
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.GCMResponseHandler
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

class SMTPEmailTopologyTest extends TopologyUTSpec {

  "SMTPEmailTopology Test" should "run" in {

    val credentials = KeyChainManager.getSimpleCredential("Flipkart-SMTP").get

    println("Credentials = " + credentials)

    val cRequest = s"""
                      |{ "id" : "123456789",
                      |	"channel": "EMAIL",
                      |	"sla": "H",
                      |	"channelData": {
                      |		"type": "EMAIL",
                      |		"subject": "Hello Kinshuk. GoodLuck!",
                      |		"text": "Text",
                      |    "html" : "<b>html</b>"
                      |
                      |	},
                      |	"channelInfo" : {
                      |	    "type" : "EMAIL",
                      |     "appName" : "FKProd",
                      |     "to" : [{ "name": "Kinshuk", "address": "kinshuk1989@gmail.com" }]
                      |	},
                      |  "clientId" : "123456",
                      |	"meta": {}
                      |}
                   """.stripMargin.getObj[ConnektRequest]


    val result = Source.single(cRequest)
      .via(new RenderFlow().flow)
      .via(new EmailChannelFormatter(64)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
      .via(new SMTPDispatcher("10.33.102.104",credentials,10).flow)
      //      .via(new GCMResponseHandler().flow)
      .runWith(Sink.ignore)

    val response = Await.result(result, 80.seconds)

    println(response)

    assert(response != null)
  }

}
