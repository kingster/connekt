package com.flipkart.connekt.busybees.streams.topologies

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.models.{GCMRequestTracker, WNSRequestTracker}
import com.flipkart.connekt.busybees.streams.ConnektTopology
import com.flipkart.connekt.busybees.streams.flows.RenderFlow
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{APNSDispatcher, GCMDispatcherPrepare, WNSDispatcherPrepare}
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters.{AndroidChannelFormatter, IOSChannelFormatter, WindowsChannelFormatter}
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.{GCMResponseHandler, WNSResponseHandler}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels._



class PushTopology(consumer: KafkaConsumerHelper) extends ConnektTopology[PNCallbackEvent] {

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = ActorMaterializer()

  override def source: Source[ConnektRequest, NotUsed] = Source.fromGraph(GraphDSL.create(){ implicit b =>
    val topics = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get
    ConnektLogger(LogFile.PROCESSORS).info(s"Creating composite source for topics: ${topics.toString()}")

    val merge = b.add(Merge[ConnektRequest](topics.size))
    for(portNum <- 0 to merge.n - 1) {
      new KafkaSource[ConnektRequest](consumer, topic = topics(portNum)) ~> merge.in(portNum)
    }

    SourceShape(merge.out)
  })

  override def transform = Flow.fromGraph(GraphDSL.create(){ implicit  b =>

    val render = b.add(new RenderFlow)
    val merger = b.add(Merge[PNCallbackEvent](3))

    val platformPartition = b.add(new Partition[ConnektRequest](3, {
      case ios if "ios".equals(ios.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"Routing IOS message: ${ios.id}")
        0
      case android if "android".equals(android.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"Routing ANDROID message: ${android.id}")
        1
      case windows if "windows".equals(windows.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"Routing WINDOWS message: ${windows.id}")
        2
    }))

    render.out ~> platformPartition.in


    /**
     * Apple Topology
     *
     * iosRequest ~> iosFormatter ~> apnsDispatcher
     */

    val fmtIOS = b.add(new IOSChannelFormatter)
    val apnsDispatcher = b.add(new APNSDispatcher)

    platformPartition.out(0) ~> fmtIOS ~> apnsDispatcher ~> merger.in(0)

    /**
     * Android Topology
     */
    val fmtAndroid = b.add(new AndroidChannelFormatter)
    val gcmHttpPrepare = b.add(new GCMDispatcherPrepare())

    val gcmPoolClientFlow = Http().cachedHostConnectionPoolHttps[GCMRequestTracker]("android.googleapis.com", 443)
    val gcmPoolFlow = b.add(gcmPoolClientFlow)

    val rHandlerGCM = b.add(new GCMResponseHandler)

    platformPartition.out(1) ~> fmtAndroid ~> gcmHttpPrepare ~> gcmPoolFlow ~> rHandlerGCM ~> merger.in(1)

    /**
     * Windows Topology
     *
     *                                          |----------|                                      |-----------|
     *                                          |          |                                      |   WNS     | ~> out-merger
     * windows request -> windows-formatter  -> |    wns   |  ~> wnsHttpPrepare ~> wnsPoolFlow ~> |  RESPONSE |
     *                                          | Payload  |                                      |  HANDLER  |
     *                                      |~~>|   Merge  |                                      |           | ~~~~~~~|
     *                                      |   |----------|                                      |-----------|        |
     *                                      |                                                                          |
     *                                      |~~~~~~~~~~~~~~~~~~~~~~~~~~~  wnsRetryMapper <~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|
     */

    val wnsPoolClientFlow = Http().cachedHostConnectionPoolHttps[WNSRequestTracker]("hk2.notify.windows.com")
    val wnsHttpPrepare = b.add(new WNSDispatcherPrepare())
    val wnsPoolFlow = b.add(wnsPoolClientFlow)

    val wnsPayloadMerge = b.add(MergePreferred[WNSPayloadEnvelope](1))
    val wnsRetryMapper = b.add(Flow[WNSRequestTracker].map(_.request))

    val fmtWindows = b.add(new WindowsChannelFormatter)
    val wnsRHandler = b.add(new WNSResponseHandler)

    platformPartition.out(2) ~>  fmtWindows ~>  wnsPayloadMerge
                                                                  wnsPayloadMerge.out ~> wnsHttpPrepare  ~> wnsPoolFlow ~> wnsRHandler.in
                                                wnsPayloadMerge.preferred <~ wnsRetryMapper <~ wnsRHandler.out1
    wnsRHandler.out0 ~> merger.in(2)

    merger.out

    FlowShape(render.in, merger.out)
  })

  override def sink: Sink[PNCallbackEvent, NotUsed] = Sink.fromGraph(GraphDSL.create(){ implicit b =>

    val evtCreator = b.add(new PNBigfootEventCreator)
    evtCreator.out ~> Sink.ignore

    SinkShape(evtCreator.in)
  })

  override def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
  }
}
