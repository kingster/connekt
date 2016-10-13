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
package com.flipkart.connekt.busybees.xmpp

import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.{ActorRef, ActorSystem, Terminated, _}
import akka.stream.FanOutShape2
import akka.stream.stage.GraphStageLogic.StageActor
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.busybees.xmpp.Internal.FreeConnectionAvailable
import com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.PendingUpstreamMessage
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{FCMXmppRequest, XmppDownstreamResponse, XmppUpstreamResponse}
import com.flipkart.connekt.commons.services.ConnektConfig

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

private[busybees] class XMPPStageLogic(val shape: FanOutShape2[(FCMXmppRequest, GCMRequestTracker), (Try[XmppDownstreamResponse], GCMRequestTracker), XmppUpstreamResponse])(implicit actorSystem: ActorSystem) extends GraphStageLogic(shape) {

  var self: StageActor = _

  private def outDownstream = shape.out0
  private def outUpstream = shape.out1

  type DownStreamResponse = (Try[XmppDownstreamResponse], GCMRequestTracker)

  private val maxStageBufferCount = ConnektConfig.get("fcm.xmpp.stageBuffer").getOrElse("10").toInt

  private val downstreamBuffer: ConcurrentLinkedQueue[DownStreamResponse] = new ConcurrentLinkedQueue[DownStreamResponse]()
  /**
    * This is discardable during shutdown, google will send back all messages which aren't acked yet
    */
  private val pendingUpstreamMessages: scala.collection.mutable.Queue[PendingUpstreamMessage] = collection.mutable.Queue[PendingUpstreamMessage]()

  private var xmppGateway: XMPPGateway = _

  override def preStart(): Unit = {
    super.preStart()

    ConnektLogger(LogFile.CLIENTS).trace("XMPPStageLogic: IN preStart")

    self = getStageActor {
      case (_, message: PendingUpstreamMessage) =>
        //ConnektLogger(LogFile.CLIENTS).trace(s"XMPPStageLogic: Inbound Message: PendingUpstreamMessage : $message")
        if (isAvailable(outUpstream))
          _pushUpstream(message)
        else
          pendingUpstreamMessages.enqueue(message)
      case (_, FreeConnectionAvailable) =>
        ConnektLogger(LogFile.CLIENTS).trace("XMPPStageLogic: Inbound Message: FreeConnectionAvailable")
        if (!hasBeenPulled(shape.in))
          pull(shape.in) //Auto Back-pressured by this.
      case (_, message: DownStreamResponse) =>
        //ConnektLogger(LogFile.CLIENTS).trace(s"XMPPStageLogic: Inbound Message: DownStreamResponse: $message")
        if (isAvailable(outDownstream))
          push(outDownstream, message)
        else
          downstreamBuffer.add(message)
      case (_, Terminated(ref)) =>
        ConnektLogger(LogFile.CLIENTS).error(s"XMPPStageLogic: Inbound Message: Terminated: $ref")
        failStage(new Exception("XMPP Router terminated : " + ref))
    }
    xmppGateway = new XMPPGateway(self)(actorSystem)

  }

  override def postStop(): Unit = {
    ConnektLogger(LogFile.CLIENTS).trace("XMPPStageLogic: IN postStop")
  }


  setHandler(shape.in, new InHandler {
    override def onPush(): Unit = {
      val requestPair: (FCMXmppRequest, GCMRequestTracker) = grab(shape.in)
      ConnektLogger(LogFile.CLIENTS).trace(s"XMPPStageLogic: IN InHandler $requestPair")
      xmppGateway(new XmppOutStreamRequest(requestPair)).onComplete {
        case Success(response) => self.ref ! Tuple2(Success(response), requestPair._2)
        case Failure(ex) => self.ref ! Tuple2(Failure(ex), requestPair._2)
      }(actorSystem.dispatcher)

      //pull more if connections available
      if (downstreamBuffer.size < maxStageBufferCount)
        pull(shape.in)
    }

    override def onUpstreamFinish(): Unit = {
      ConnektLogger(LogFile.CLIENTS).trace("GcmXmppDispatcher: upstream finished:shutting down")
      xmppGateway.shutdown()

      ConnektLogger(LogFile.CLIENTS).trace("GcmXmppDispatcher :upstream finished:downstreamcount:" + downstreamBuffer.size())
      emitMultiple[DownStreamResponse](outDownstream, downstreamBuffer.iterator.asScala, () => {
        ConnektLogger(LogFile.CLIENTS).trace("GcmXmppDispatcher :emitted all:" + downstreamBuffer.size())
        completeStage() //Mark stage as completed when the emit current buffer completes.
      })

      ConnektLogger(LogFile.CLIENTS).trace("GcmXmppDispatcher :shutdown complete")
    }
  })

  //downstream act
  setHandler(outDownstream, new OutHandler {
    override def onPull(): Unit = {
      if (!downstreamBuffer.isEmpty) {
        push(outDownstream, downstreamBuffer.poll())
      }
      if (!hasBeenPulled(shape.in)){
        ConnektLogger(LogFile.CLIENTS).trace("GcmXmppDispatcher :outDownstream PULL")
        pull(shape.in) //TODO : This should have a check that there are free routess
      }

    }
  })

  //upstream
  setHandler(outUpstream, new OutHandler {
    override def onPull(): Unit = {
      if (pendingUpstreamMessages.nonEmpty) {
        val upstreamMessage = pendingUpstreamMessages.dequeue()
        _pushUpstream(upstreamMessage)
      }
    }
  })

  private def _pushUpstream(upstreamMessage:PendingUpstreamMessage): Unit ={
    upstreamMessage.actTo ! upstreamMessage.message
    push(outUpstream, upstreamMessage.message)
  }
}

