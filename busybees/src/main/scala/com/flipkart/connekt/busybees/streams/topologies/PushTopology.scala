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
import com.flipkart.connekt.busybees.streams.flows.profilers.TimedFlowOps._
import com.flipkart.connekt.busybees.streams.flows.reponsehandlers._
import com.flipkart.connekt.busybees.streams.flows.{FlowMetrics, RenderFlow}
import com.flipkart.connekt.busybees.streams.sources.KafkaSource
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.entities.{MobilePlatform, Channel}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.Config

import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise


class PushTopology(kafkaConsumerConfig: Config) extends ConnektTopology[PNCallbackEvent] with SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.CLIENT_QUEUE_CREATE))

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = BusyBeesBoot.mat
  val ioMat = BusyBeesBoot.ioMat

  val ioDispatcher = system.dispatchers.lookup("akka.actor.io-dispatcher")

  val sourceSwitches: scala.collection.mutable.ListBuffer[Promise[String]] = ListBuffer()

  override def source(checkpointGroup: CheckPointGroup): Source[ConnektRequest, NotUsed] = Source.fromGraph(GraphDSL.create() { implicit b =>
    val topics = ServiceFactory.getPNMessageService.getTopicNames(Channel.PUSH).get
    val groupId = kafkaConsumerConfig.getString("group.id")

    ConnektLogger(LogFile.PROCESSORS).info(s"Creating composite source for topics: ${topics.toString()}")

    val merge = b.add(Merge[ConnektRequest](topics.size))

    for (portNum <- 0 until merge.n) {
      val p = Promise[String]()
      val consumerGroup = s"${groupId}_$checkpointGroup"
      new KafkaSource[ConnektRequest](kafkaConsumerConfig, topic = topics(portNum), consumerGroup)(p.future) ~> merge.in(portNum)
      sourceSwitches += p
    }

    SourceShape(merge.out)
  })

  def androidTransformFlow = Flow.fromGraph(GraphDSL.create() { implicit b =>

    /**
     * Android Topology
     *                      +---------------+     +----------------+      +-------------------+      +-----------------+     +--------------------+      +--..
     *  ConnektRequest ---> | AndroidFilter | --> |AndroidFormatter| ---> | GCMRequestPrepare |  --> |  GCMDispatcher  | --> | GCMResponseHandler | ---> |Merger
     *                      +---------------+     +----------------+      +-------------------+      +-----------------+     +--------------------+      +-----
     */

    val render = b.add(new RenderFlow().flow)
    val fmtAndroidParallelism = ConnektConfig.getInt("topology.push.androidFormatter.parallelism").get
    val androidFilter = b.add(Flow[ConnektRequest].filter(m => MobilePlatform.ANDROID.toString.equalsIgnoreCase(m.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase)))
    val fmtAndroid = b.add(new AndroidChannelFormatter(fmtAndroidParallelism)(ioDispatcher).flow)
    val gcmHttpPrepare = b.add(new GCMDispatcherPrepare().flow)
    val gcmPoolFlow = b.add(HttpDispatcher.gcmPoolClientFlow.timedAs("gcmRTT"))
    val gcmResponseHandle = b.add(new GCMResponseHandler()(ioMat, ioDispatcher).flow)

    render.out ~> androidFilter ~> fmtAndroid ~> gcmHttpPrepare ~> gcmPoolFlow ~> gcmResponseHandle

    FlowShape(render.in, gcmResponseHandle.out)
  })

  def iosTransformFlow = Flow.fromGraph(GraphDSL.create() { implicit b =>

    /**
     * IOS Topology
     *                      +---------------+     +----------------+      +------------------+      +-----------------+     +-------------------+      +--..
     *  ConnektRequest ---> |   IOSFilter   | --> |  IOSFormatter  | ---> |APNSRequestPrepare|  --> | APNSDispatcher  | --> |APNSResponseHandler| ---> |Merger
     *                      +---------------+     +----------------+      +------------------+      +-----------------+     +-------------------+      +-------
     */

    val render = b.add(new RenderFlow().flow)
    val fmtIOSParallelism = ConnektConfig.getInt("topology.push.iosFormatter.parallelism").get
    val apnsDispatcherParallelism = ConnektConfig.getInt("topology.push.apnsDispatcher.parallelism").getOrElse(1024)
    val iosFilter = b.add(Flow[ConnektRequest].filter(m => MobilePlatform.IOS.toString.equalsIgnoreCase(m.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase)))
    val fmtIOS = b.add(new IOSChannelFormatter(fmtIOSParallelism)(ioDispatcher).flow)
    val apnsPrepare = b.add(new APNSDispatcherPrepare().flow)
    val apnsDispatcher = b.add(new APNSDispatcher(apnsDispatcherParallelism)(ioDispatcher).flow.timedAs("apnsRTT"))
    val apnsResponseHandle = b.add(new APNSResponseHandler().flow)

    render.out ~> iosFilter ~> fmtIOS ~> apnsPrepare ~> apnsDispatcher ~> apnsResponseHandle

    FlowShape(render.in, apnsResponseHandle.out)
  })

  def windowsTransformFlow = Flow.fromGraph(GraphDSL.create() { implicit b =>

    /**
     * Windows Topology
     *
     *                      +---------------+     +----------------+       +----------+     +-----------------+      +-----------------+     +------------------+     +-------------------------+          +--..
     *  ConnektRequest ---> | WindowsFilter | --> |WindowsFormatter|-+---> |  Merger  | --> |WNSRequestPrepare|  --> |  WNSDispatcher  | --> |WNSResponseHandler| --> |Response / Error Splitter| --+----> |Merger
     *                      +---------------+     +----------------+ |     +----------+     +-----------------+      +-----------------+     +------------------+     +-------------------------+   |      +-----
     *                                                               +------------------------------------------------------------------------------------------------------------------------------+
     */

    val render = b.add(new RenderFlow().flow)
    val windowsFilter = b.add(Flow[ConnektRequest].filter(m => MobilePlatform.WINDOWS.toString.equalsIgnoreCase(m.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase)))
    val wnsHttpPrepare = b.add(new WNSDispatcherPrepare().flow)
    val wnsPoolFlow = b.add(HttpDispatcher.wnsPoolClientFlow.timedAs("wnsRTT"))

    val wnsPayloadMerge = b.add(MergePreferred[WNSPayloadEnvelope](1))
    val wnsRetryMapper = b.add(Flow[WNSRequestTracker].map(_.request) /*.buffer(10, OverflowStrategy.backpressure)*/)

    val fmtWindowsParallelism = ConnektConfig.getInt("topology.push.windowsFormatter.parallelism").get
    val fmtWindows = b.add(new WindowsChannelFormatter(fmtWindowsParallelism)(ioDispatcher).flow)

    val wnsRHandler = b.add(new WNSResponseHandler()(ioMat, ec).flow)

    val wnsRetryPartition = b.add(new Partition[Either[WNSRequestTracker, PNCallbackEvent]](2, {
      case Right(_) => 0
      case Left(_) => 1
    }))

    render.out ~> windowsFilter ~>  fmtWindows ~>  wnsPayloadMerge
    wnsPayloadMerge.out ~> wnsHttpPrepare  ~> wnsPoolFlow ~> wnsRHandler ~> wnsRetryPartition.in
    wnsPayloadMerge.preferred <~ wnsRetryMapper <~ wnsRetryPartition.out(1).map(_.left.get).outlet

    FlowShape(render.in, wnsRetryPartition.out(0).map(_.right.get).outlet)
  })

  def openWebTransformFlow = Flow.fromGraph(GraphDSL.create() { implicit b =>

    /**
     * OpenWeb Topology
     *
     *                      +---------------+     +----------------+      +---------------------+      +-------------------+     +----------------------+      +--..
     *  ConnektRequest ---> | OpenWebFilter | --> |OpenWebFormatter| ---> |OpenWebRequestPrepare|  --> | OpenWebDispatcher | --> |OpenWebResponseHandler| ---> |Merger
     *                      +---------------+     +----------------+      +---------------------+      +-------------------+     +----------------------+      +-----
     */

    val render = b.add(new RenderFlow().flow)
    val fmtOpenWebParallelism = ConnektConfig.getInt("topology.push.openwebFormatter.parallelism").get
    val openWebFilter = b.add(Flow[ConnektRequest].filter(m => MobilePlatform.OPENWEB.toString.equalsIgnoreCase(m.channelInfo.asInstanceOf[PNRequestInfo].platform.toLowerCase)))
    val fmtOpenWeb = b.add(new OpenWebChannelFormatter(fmtOpenWebParallelism)(ioDispatcher).flow)

    //providers [standard]
    val openWebHttpPrepare = b.add(new OpenWebDispatcherPrepare().flow)
    val openWebPoolFlow = b.add(HttpDispatcher.openWebPoolClientFlow.timedAs("openWebRTT"))
    val openWebResponseHandle = b.add(new OpenWebResponseHandler().flow)

    render.out ~> openWebFilter ~> fmtOpenWeb ~> openWebHttpPrepare ~> openWebPoolFlow ~> openWebResponseHandle

    FlowShape(render.in, openWebResponseHandle.out)
  })

  override def sink: Sink[PNCallbackEvent, NotUsed] = Sink.fromGraph(GraphDSL.create() { implicit b =>

    /**
     * Sink Topology
     *
     *                        +---------------------+     +-----------------+          ...-----+
     *   PNCallbackEvent ---> | BigfootEventCreator | --> | MetricsRecorder | --->  IgnoreSink |
     *                        +---------------------+     +-----------------+      +-----------+
     */

    val evtCreator = b.add(new PNBigfootEventCreator)
    val metrics = b.add(new FlowMetrics[fkint.mp.connekt.PNCallbackEvent](Channel.PUSH).flow)
    evtCreator.out ~> metrics ~> Sink.ignore

    SinkShape(evtCreator.in)
  })

  override def shutdown() = {
    /* terminate in top-down approach from all Source(s) */
    sourceSwitches.foreach(_.success("PushTopology signal source shutdown"))
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Any = {
    _type match {
      case SyncType.CLIENT_QUEUE_CREATE => Try_ {
        ConnektLogger(LogFile.SERVICE).info(s"Busybees Restart for CLIENT_QUEUE_CREATE Client: ${args.head}, New Topic: ${args.last} ")
        restart
      }
      case _ =>
    }
  }

  override def transformers: Map[CheckPointGroup, Flow[ConnektRequest, PNCallbackEvent, NotUsed]] = {
    Map(MobilePlatform.IOS.toString -> iosTransformFlow,
      MobilePlatform.ANDROID.toString -> androidTransformFlow,
      MobilePlatform.WINDOWS.toString -> windowsTransformFlow,
      MobilePlatform.OPENWEB.toString -> openWebTransformFlow
    )
  }
}
