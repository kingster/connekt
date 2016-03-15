package com.flipkart.connekt.busybees.streams.topologies

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.streams.ConnektTopology
import com.flipkart.connekt.busybees.streams.flows.dispatchers.{APNSDispatcher, GCMDispatcherPrepare, HttpDispatcher, WNSDispatcherPrepare}
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters.{AndroidChannelFormatter, IOSChannelFormatter, WindowsChannelFormatter}
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers.{GCMResponseHandler, WNSResponseHandler}
import com.flipkart.connekt.busybees.streams.flows.{FlowMetrics, RenderFlow}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise


class PushTopology(consumer: KafkaConsumerHelper) extends ConnektTopology[PNCallbackEvent] {

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = BusyBeesBoot.mat

  val ioDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")

  var sourceSwitches: List[Promise[String]] = _

  override def source: Source[ConnektRequest, NotUsed] = Source.fromGraph(GraphDSL.create(){ implicit b =>
    val topics = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get

    val partitionFactor = ConnektConfig.getInt("busybees.connections.kafka.topic.partitionFactor").get
    ConnektLogger(LogFile.PROCESSORS).info(s"Creating composite source for topics: ${topics.toString()}")

    val merge = b.add(Merge[ConnektRequest](topics.size))
    val handles = ListBuffer[Promise[String]]()

    for(portNum <- 0 to merge.n - 1) {
      val p = Promise[String]()
      new KafkaSource[ConnektRequest](consumer, topic = topics(portNum), partitionFactor)(p.future) ~> merge.in(portNum)
      handles += p
    }

    sourceSwitches = handles.toList

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

    val fmtIOSParallelism = ConnektConfig.getInt("topology.push.iosFormatter.parallelism").get
    val fmtIOS = b.add(new IOSChannelFormatter(fmtIOSParallelism)(ioDispatcher).flow)
    val apnsDispatcher = b.add(new APNSDispatcher)

    platformPartition.out(0) ~> fmtIOS ~> apnsDispatcher ~> merger.in(0)

    /**
     * Android Topology
     */
    val fmtAndroidParallelism = ConnektConfig.getInt("topology.push.androidFormatter.parallelism").get
    val fmtAndroid = b.add(new AndroidChannelFormatter(fmtAndroidParallelism)(ioDispatcher).flow)
    val gcmHttpPrepare = b.add(new GCMDispatcherPrepare())

    val gcmPoolFlow = b.add(HttpDispatcher.gcmPoolClientFlow)

    val gcmResponseHandle = b.add(new GCMResponseHandler())

    platformPartition.out(1) ~> fmtAndroid ~> gcmHttpPrepare ~> gcmPoolFlow ~> gcmResponseHandle ~> merger.in(1)

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

    val wnsHttpPrepare = b.add(new WNSDispatcherPrepare())
    val wnsPoolFlow = b.add(HttpDispatcher.wnsPoolClientFlow)

    val wnsPayloadMerge = b.add(MergePreferred[WNSPayloadEnvelope](1))
    val wnsRetryMapper = b.add(Flow[WNSRequestTracker].map(_.request)/*.buffer(10, OverflowStrategy.backpressure)*/)

    val fmtWindowsParallelism = ConnektConfig.getInt("topology.push.windowsFormatter.parallelism").get
    val fmtWindows = b.add(new WindowsChannelFormatter(fmtWindowsParallelism)(ioDispatcher).flow)
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
    val metrics = b.add(new FlowMetrics[fkint.mp.connekt.PNCallbackEvent](Channel.PUSH))
    evtCreator.out ~> metrics ~> Sink.ignore

    SinkShape(evtCreator.in)
  })

  override def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
    sourceSwitches.foreach(_.success("PushTopology:: signal source shutdown."))
  }
}
