package com.flipkart.connekt.busybees.tests.streams.topologies

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

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.{Condition, ReentrantLock}

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{Merge, GraphDSL, Sink, Source}
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{GcmXmppDispatcher, GCMXmppDispatcherPrepare}
import com.flipkart.connekt.busybees.streams.flows.formaters.{AndroidXmppChannelFormatter}
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.{XmppUpstreamHandler, XmppDownstreamHandler}
import com.flipkart.connekt.busybees.tests.streams.TopologyUTSpec
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.iomodels.{PNCallbackEvent, ConnektRequest}
import com.flipkart.connekt.commons.services.{DeviceDetailsService, KeyChainManager}
import com.flipkart.connekt.commons.utils.StringUtils._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.stage.{OutHandler, GraphStageLogic, GraphStage}
import scala.concurrent.Await
import scala.concurrent.duration._

class XmppMessageSource(lock:ReentrantLock, conditionLock: Condition)  extends GraphStage[SourceShape[ConnektRequest]]{
  // Define the (sole) output port of this stage
  val out: Outlet[ConnektRequest] = Outlet("RandomMessageSource")
  // Define the shape of this stage, which is SourceShape with the port we defined above
  override val shape: SourceShape[ConnektRequest] = SourceShape(out)
  val counter = new AtomicInteger(0)
  var sendNow:Boolean = false

  // This is where the actual (possibly stateful) logic will live
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        println("IN ON PULL")
        lock.lock()
        try {
          while ( sendNow == false )
            conditionLock.await()

          sendNow = false
        } finally {
          lock.unlock()
        }

        val nowCounter = counter.incrementAndGet()
        ConnektLogger(LogFile.CLIENTS).debug("Request count:" + nowCounter)
        val cRequest = s"""
                          |{
                          |"id": "${System.currentTimeMillis()}",
                          |	"channel": "PN",
                          |	"sla": "H",
                          |	"stencilId": "retail-app-base-0x23",
                          |	"scheduleTs": 12312312321,
                          | "clientId":"xmppTest",
                          |	"channelData": {
                          |		"type": "PN",
                          |		"data": {
                          |			"message": "Hello Kinshuk. GoodLuck!!!! ${nowCounter}",
                          |			"title": "Kinshuk GCM Push Test..... ${nowCounter}",
                          |			"id": "${System.currentTimeMillis()}",
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
                          |     "appName" : "ConnektSampleApp",
                          |     "deviceIds" : ["subirDeviceForXmppTest"]
                          |	},
                          |	"meta": {}
                          |}
                   """.stripMargin.getObj[ConnektRequest]

        push(out, cRequest)
      }
    })
  }
}

class AndroidXmppTopologyTest extends TopologyUTSpec {


  "AndroidXmppTopology Test" should "run" in {

    val credentials = KeyChainManager.getGoogleCredential("ConnektSampleApp").get

    DeviceDetailsService.add(
      DeviceDetails(
        deviceId = "subirDeviceForXmppTest",
        userId = "",
        token = "dbp9UlEJTbg:APA91bHZxuX6BnPLW4J_NZSLh7TeXN00eOGZY99npmVBVpB8MVciZDHlhOL4F7RRCktfcYQf90_7HgsWvLLh8C4aavinu6Mp0nt3RKCvy0EaopnRDqK_XwFwTeOkb8SAeQUCBwbmGPXA",
        osName = "android", osVersion = "6.0.1", appName = "ConnektSampleApp", appVersion = "UT", brand = "", model = ""
      )
    )

    val ioDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")

    lazy val xmppOnlyTopology = GraphDSL.create() {
      implicit b ⇒
        val fmtAndroid = b.add(new AndroidXmppChannelFormatter(64)(ioDispatcher).flow)
        val gcmXmppPrepare = b.add(new GCMXmppDispatcherPrepare().flow)
        val gcmXmppPoolFlow = b.add(new GcmXmppDispatcher)
        val downstreamHandler = b.add(new XmppDownstreamHandler()(mat, ioDispatcher).flow)
        val upstreamHandler = b.add(new XmppUpstreamHandler()(mat, ioDispatcher).flow)
        val merger = b.add(Merge[PNCallbackEvent](2))

        fmtAndroid ~> gcmXmppPrepare ~> gcmXmppPoolFlow.in
                                            gcmXmppPoolFlow.out0 ~> downstreamHandler ~> merger.in(0)
                                            gcmXmppPoolFlow.out1 ~> upstreamHandler ~> merger.in(1)
        FlowShape(fmtAndroid.in, merger.out)
    }

    val lock = new ReentrantLock()
    val conditionLock = lock.newCondition()
    val sourceGraph = new XmppMessageSource(lock, conditionLock)
    val result = Source.fromGraph(sourceGraph)
                   .via(xmppOnlyTopology)
                   .runWith(Sink.ignore)

    //this is to send one message for push but leave input stream open untill that xmpp ACK and Receipt received. Then kill manually.
    //do it in loop to send multiple
    lock.lock()
    sourceGraph.sendNow = true
    conditionLock.signal()
    lock.unlock()

    val response = Await.result(result, 360.seconds)

    println(response)

    assert(response != null)
  }

}
