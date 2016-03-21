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
import akka.http.scaladsl.model._
import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{APNSDispatcher, GCMDispatcherPrepare}
import com.flipkart.connekt.busybees.streams.flows.formaters.IOSChannelFormatter
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.{DeviceDetailsService, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.util.Try

class PNCompleteTopologyTest extends TopologyUTSpec {

  "PNCompleteTopology Test" should "run" in {

    val deviceId1 = "VM6DODCT7BEOO0LGMH1Q399O96LPMRYE"

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId1,
        userId = "",
        token = "6b1e059bb2a51d03d37384d1493aaffbba4edc58f8e21eb2f80ad4851875ee25",
        osName = "ios", osVersion = "6.0.1", appName = "UT", appVersion = "UT", brand = "", model = ""
      )
    )

    val deviceId2 = "EUS6K7VPSR0J26GHOUTA7SJ6OB7SXZ97"

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = deviceId2,
        userId = "",
        token = "eykALNwQEV8:APA91bHQGNh5ThFyeJuUVoApqjU1qoTyvMA3yT77553tHpB0G0lzJQicBKJIjSRQ12uP7QNJauZjgDvlMYWHdYWZ0mBZVgOP1aX46w9mqP-Oy-dXtV0iEENthWxetgEayGGM6nGPBkU",
        osName = "android", osVersion = "6.0.1", appName = "UT", appVersion = "UT", brand = "", model = ""
      )
    )

    val cRequest1 =
      """
        |{
        |	"id": "REQUEST-VM6DODCT7BEOO0LGMH1Q399O96LPMRYE",
        |	"channel": "PN",
        |	"sla": "H",
        |	"templateId": "retail-app-base-0x23",
        |	"scheduleTs": 12312312321,
        |	"expiryTs": 3243243224,
        |	"channelData": {
        |		"type": "PN",
        |		"data": {
        |			"alert": "Message received from Kinshuk 1234",
        |			"sound": "default",
        |			"badge": 0
        |		}
        |	},
        |	"channelInfo": {
        |		"type": "PN",
        |		"ackRequired": true,
        |		"delayWhileIdle": true,
        |		"platform": "ios",
        |		"appName": "UT",
        |		"deviceId": [ "VM6DODCT7BEOO0LGMH1Q399O96LPMRYE" ]
        |	},
        |	"meta": {}
        |}
      """.stripMargin.getObj[ConnektRequest]

    val cRequest2 =
      """
        |{
        |	"id": "REQUEST-EUS6K7VPSR0J26GHOUTA7SJ6OB7SXZ97",
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
        |	"channelInfo": {
        |		"type": "PN",
        |		"ackRequired": true,
        |		"delayWhileIdle": true,
        |		"platform": "android",
        |		"appName": "UT",
        |		"deviceId": [ "EUS6K7VPSR0J26GHOUTA7SJ6OB7SXZ97" ]
        |	},
        |	"meta": {}
        |}
      """.stripMargin.getObj[ConnektRequest]


    val credentials = KeyChainManager.getGoogleCredential("ConnektSampleApp").get
    val appleCredentials = KeyChainManager.getAppleCredentials("RetailApp").get

    val httpDispatcher = new GCMDispatcherPrepare

    lazy implicit val poolClientFlow = Http().cachedHostConnectionPoolHttps[String]("android.googleapis.com", 443)


    val httpReader = Flow[(Try[HttpResponse], String)].map(input => {
      input._2
    })


    val channelPartition = new Partition[ConnektRequest](2, {
      case ios if "ios".equals(ios.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) => 0
      case android if "android".equals(android.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) => 1
    })



    lazy val graph = GraphDSL.create() {
      implicit b ⇒

        val out = Sink.foreach[String](println)

        val render = b.add(new RenderFlow)

        val iosFormat = b.add(new IOSChannelFormatter(16)(system.dispatchers.lookup("akka.actor.io-dispatcher")).flow)
        val iosDispatch = b.add(new APNSDispatcher)

        //        val androidFormat = b.add(new AndroidChannelFormatter)
        //        val httpPrepare = b.add(httpDispatcher)
        //        val httpSend = b.add(poolClientFlow)
        //        val hRead = b.add(httpReader)


        val resultsMerge = b.add(Merge[String](2))

        val partition = b.add(channelPartition)

        /**
         * WHy do I need to do this? I don't know, without this proxy's merge throws
         * some crazy type error which I don't know what to do! Someone fix this
         */
        val proxy1 = b.add(Flow[Either[Throwable, String]].filter(_.isRight).map(_.toString))
        val proxy2 = b.add(Flow[ConnektRequest].map(_.toString))
        val proxy3 = b.add(Flow[PNCallbackEvent].map(_.toString))


        Source(List(cRequest1, cRequest2)) ~> render ~> partition.in
        partition.out(0) ~> iosFormat ~> iosDispatch ~> proxy3 ~> resultsMerge.in(0)
        //        partition.out(0) ~> proxy1 ~> resultsMerge.in(0)
        //        partition.out(1) ~> new AndroidChannelFormatter  ~> httpDispatcher ~> poolClientFlow ~> hRead ~> resultsMerge.in(1)
        partition.out(1) ~> proxy2 ~> resultsMerge.in(1)

        resultsMerge.out ~> out

        ClosedShape
    }




    RunnableGraph.fromGraph(graph).run()

    //    val response = Await.result(result, 60.seconds)
    //
    Thread.sleep(20000)
    //
    //    assert(null != response)


  }

}
