package com.flipkart.connekt.busybees.streams.topologies

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.{Sink, Source}
import com.flipkart.connekt.busybees.streams.TopologyUTSpec
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.HttpPrepare
import com.flipkart.connekt.busybees.streams.flows.formaters.AndroidChannelFormatter
import com.flipkart.connekt.busybees.streams.sources.RateControl
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, GCMPayload}
import com.flipkart.connekt.commons.services.CredentialManager
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by kinshuk.bairagi on 05/02/16.
 */
class AndroidTopology extends TopologyUTSpec {

  "AndroidTopology Test" should "run" in {

    val credentials = CredentialManager.getCredential("PN.ConnektSampleApp")

    val httpDispatcher = new HttpPrepare[GCMPayload](
      new URL("https", "android.googleapis.com", 443, "/gcm/send"),
      HttpMethods.POST,
      scala.collection.immutable.Seq[HttpHeader](RawHeader("Authorization", "key=" + credentials.password)),
      (payload: GCMPayload) => HttpEntity(ContentTypes.`application/json`, payload.getJson)
    )


    lazy implicit val poolClientFlow = Http().cachedHostConnectionPoolTls[String]("android.googleapis.com", 443)

    val cRequest = """
                     |{
                     |	"channel": "PN",
                     |	"sla": "H",
                     |	"templateId": "retail-app-base-0x23",
                     |	"scheduleTs": 12312312321,
                     |	"expiryTs": 3243243224,
                     |	"channelData": {
                     |		"type": "PN",
                     |		"data": {
                     |			"message": "Hello Kinshuk. GoodLuck!",
                     |			"title": "Kinshuk GCM Push Test",
                     |			"id": "123456789",
                     |			"triggerSound": true,
                     |			"notificationType": "Text"
                     |
                     |		}
                     |	},
                     |	"channelInfo" : {
                     |	    "type" : "PN",
                     |	    "ackRequired": true,
                     |    	"delayWhileIdle": true,
                     |     "platform" :  "android",
                     |     "appName" : "ConnectSampleApp",
                     |     "deviceId" : "513803e45cf1b344ef494a04c9fb650a"
                     |	},
                     |	"meta": {}
                     |}
                   """.stripMargin.getObj[ConnektRequest]


    val result = Source.single(cRequest)
      .via(new RateControl[ConnektRequest](2, 1, 2))
      .via(new RenderFlow)
      .via(new AndroidChannelFormatter)
      .via(httpDispatcher)
      .via(poolClientFlow)
      .runWith(Sink.head)

    val response = Await.result(result, 60.seconds)

    response._1.isSuccess shouldEqual true

    val httpResponse = Await.result(response._1.get.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)

    println(httpResponse)

    assert(httpResponse != null)


  }

}
