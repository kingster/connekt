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
package com.flipkart.connekt.busybees.streams.topologies

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.busybees.models.WNSRequestTracker
import com.flipkart.connekt.busybees.streams.ConnektTopology
import com.flipkart.connekt.busybees.streams.flows.dispatchers._
import com.flipkart.connekt.busybees.streams.flows.eventcreators.PNBigfootEventCreator
import com.flipkart.connekt.busybees.streams.flows.formaters._
import com.flipkart.connekt.busybees.streams.flows.partitioner.OpenWebProviderPartitioner
import com.flipkart.connekt.busybees.streams.flows.profilers.TimedFlowOps._
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers._
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
  val ioMat = BusyBeesBoot.ioMat

  val ioDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")

  var sourceSwitches: List[Promise[String]] = _

  override def source: Source[ConnektRequest, NotUsed] = Source.fromGraph(GraphDSL.create(){ implicit b =>
    val topics = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get

    ConnektLogger(LogFile.PROCESSORS).info(s"Creating composite source for topics: ${topics.toString()}")

    val merge = b.add(Merge[ConnektRequest](topics.size))
    val handles = ListBuffer[Promise[String]]()

    for(portNum <- 0 to merge.n - 1) {
      val p = Promise[String]()
      new KafkaSource[ConnektRequest](consumer, topic = topics(portNum))(p.future) ~> merge.in(portNum)
      handles += p
    }

    sourceSwitches = handles.toList

    SourceShape(merge.out)
  })

  override def transform = Flow.fromGraph(GraphDSL.create(){ implicit  b =>

    val render = b.add(new RenderFlow().flow)
    val merger = b.add(Merge[PNCallbackEvent](4))

    val platformPartition = b.add(new Partition[ConnektRequest](4, {
      case ios if "ios".equals(ios.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"routing ios message: ${ios.id}")
        0
      case android if "android".equals(android.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"routing android message: ${android.id}")
        1
      case windows if "windows".equals(windows.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"routing windows message: ${windows.id}")
        2
      case openweb if "openweb".equals(openweb.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase) =>
        ConnektLogger(LogFile.PROCESSORS).debug(s"routing openweb message: ${openweb.id}")
        3
    }))

    render.out ~> platformPartition.in


    /**
     * Apple Topology
     *
     * iosRequest ~> iosFormatter ~> apnsDispatcher
     */

    val fmtIOSParallelism = ConnektConfig.getInt("topology.push.iosFormatter.parallelism").get
    val apnsDispatcherParallelism = ConnektConfig.getInt("topology.push.apnsDispatcher.parallelism").getOrElse(1024)
    val fmtIOS = b.add(new IOSChannelFormatter(fmtIOSParallelism)(ioDispatcher).flow)
    val apnsPrepare = b.add(new APNSDispatcherPrepare().flow)
    val apnsDispatcher = b.add(new APNSDispatcher(apnsDispatcherParallelism)(ioDispatcher).flow.timedAs("apnsRTT"))
    val apnsResponseHandle = b.add(new APNSResponseHandler().flow)

    platformPartition.out(0) ~> fmtIOS ~> apnsPrepare ~> apnsDispatcher ~> apnsResponseHandle ~> merger.in(0)

    /**
     * Android Topology
     */
    val fmtAndroidParallelism = ConnektConfig.getInt("topology.push.androidFormatter.parallelism").get
    val fmtAndroid = b.add(new AndroidChannelFormatter(fmtAndroidParallelism)(ioDispatcher).flow)
    val gcmHttpPrepare = b.add(new GCMDispatcherPrepare().flow)

    val gcmPoolFlow = b.add(HttpDispatcher.gcmPoolClientFlow.timedAs("gcmRTT"))

    val gcmResponseHandle = b.add(new GCMResponseHandler()(ioMat, ioDispatcher).flow)

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

    val wnsHttpPrepare = b.add(new WNSDispatcherPrepare().flow)
    val wnsPoolFlow = b.add(HttpDispatcher.wnsPoolClientFlow.timedAs("wnsRTT"))

    val wnsPayloadMerge = b.add(MergePreferred[WNSPayloadEnvelope](1))
    val wnsRetryMapper = b.add(Flow[WNSRequestTracker].map(_.request)/*.buffer(10, OverflowStrategy.backpressure)*/)

    val fmtWindowsParallelism = ConnektConfig.getInt("topology.push.windowsFormatter.parallelism").get
    val fmtWindows = b.add(new WindowsChannelFormatter(fmtWindowsParallelism)(ioDispatcher).flow)
    val wnsRHandler = b.add(new WNSResponseHandler()(ioMat, ec))

    platformPartition.out(2) ~>  fmtWindows ~>  wnsPayloadMerge
                                                                  wnsPayloadMerge.out ~> wnsHttpPrepare  ~> wnsPoolFlow ~> wnsRHandler.in
                                                wnsPayloadMerge.preferred <~ wnsRetryMapper <~ wnsRHandler.out1
    wnsRHandler.out0 ~> merger.in(2)


    /**
     * OpenWeb only chrome support without data for now
     * TODO: Add data support
     */

    val fmtOpenWebParallelism = ConnektConfig.getInt("topology.push.openwebFormatter.parallelism").get
    val fmtOpenWeb = b.add(new OpenWebChannelFormatter(fmtOpenWebParallelism)(ioDispatcher).flow)
    val openWebProviderPart = b.add(new OpenWebProviderPartitioner)
    val openWebMerger = b.add(Merge[PNCallbackEvent](2))

    //providers
    val openWebGenericProvider = b.add(HttpDispatcher.openWebStandardClientFlow)

    //gcm provider for openweb
    val gcmHttpPrepare2 = b.add(new GCMDispatcherPrepare().flow)
    val gcmPoolFlow2 = b.add(HttpDispatcher.gcmPoolClientFlow.timedAs("openWebGoogleRTT"))
    val gcmResponseHandle2 = b.add(new GCMResponseHandler()(ioMat, ioDispatcher).flow)

    platformPartition.out(3) ~> fmtOpenWeb ~> openWebProviderPart.in
                                              openWebProviderPart.out0 ~> gcmHttpPrepare2 ~> gcmPoolFlow2 ~> gcmResponseHandle2 ~> openWebMerger.in(0)
                                              openWebProviderPart.out1 ~> openWebGenericProvider ~> openWebMerger.in(1)

    openWebMerger.out ~>  merger.in(3)

    FlowShape(render.in, merger.out)
  })

  override def sink: Sink[PNCallbackEvent, NotUsed] = Sink.fromGraph(GraphDSL.create(){ implicit b =>

    val evtCreator = b.add(new PNBigfootEventCreator)
    val metrics = b.add(new FlowMetrics[fkint.mp.connekt.PNCallbackEvent](Channel.PUSH).flow)
    evtCreator.out ~> metrics ~> Sink.ignore

    SinkShape(evtCreator.in)
  })

  override def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
    sourceSwitches.foreach(_.success("PushTopology signal source shutdown"))
  }
}
