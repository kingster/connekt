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
package com.flipkart.connekt.busybees.streams.flows.partitioner

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FanOutShape2, Inlet, Outlet}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GCMPayloadEnvelope, OpenWebPayloadEnvelope, PayloadEnvelope}

class OpenWebProviderPartitioner extends  GraphStage[FanOutShape2[PayloadEnvelope, GCMPayloadEnvelope, OpenWebPayloadEnvelope]] {

  val in = Inlet[PayloadEnvelope]("OpenWebProviderPartitioner.In")
  val outGoogle = Outlet[GCMPayloadEnvelope]("OpenWebProviderPartitioner.OutChrome")
  val outGeneric = Outlet[OpenWebPayloadEnvelope]("OpenWebProviderPartitioner.OutGeneric")

  override def shape = new FanOutShape2(in, outGoogle, outGeneric)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape ) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val data = grab(in)
        data match {
          case gcm:GCMPayloadEnvelope => push(outGoogle, data.asInstanceOf[GCMPayloadEnvelope])
          case other: OpenWebPayloadEnvelope => push(outGeneric, data.asInstanceOf[OpenWebPayloadEnvelope])
        }
      }
    })

    setHandler(outGoogle, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).trace(s"OpenWebProviderPartitioner pulled upstream on OpenWebProviderPartitioner.OutChrome pull")
        }
      }
    })

    setHandler(outGeneric, new OutHandler {
      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          pull(in)
          ConnektLogger(LogFile.PROCESSORS).trace(s"OpenWebProviderPartitioner pulled upstream on OpenWebProviderPartitioner.OutGeneric pull")
        }
      }
    })
  }

}
