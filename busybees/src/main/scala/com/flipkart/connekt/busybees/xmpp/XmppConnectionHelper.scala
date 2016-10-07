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

import akka.actor.{ActorSystem, DeadLetterSuppression}
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.commons.iomodels.{FCMXmppRequest, XmppNack}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.typesafe.config.Config
import org.jivesoftware.smack.tcp.XMPPTCPConnection

import scala.util.control.NoStackTrace

object XmppConnectionHelper {

  lazy val xmppHost: String = ConnektConfig.get("fcm.ccs.fcmServer").getOrElse("fcm-xmpp.googleapis.com")
  lazy val xmppPort: Int = ConnektConfig.get("fcm.ccs.fcmPort").getOrElse("5235").toInt

}

private[xmpp] object Internal {

  final case class StoppingException() extends RuntimeException("XMPP Connection is Stopping/Stopped")
  case object FreeConnectionAvailable
  case object ConnectionBusy
  case object XmppRequestAvailable
  case class ConnectionClosed(connection: XMPPTCPConnection) extends DeadLetterSuppression
  case class ReSize(count: Int)

  final val GCM_NAMESPACE: String = "google:mobile:data"
  final val GCM_ELEMENT_NAME: String = "gcm"

}

case class XmppOutStreamRequest(request: FCMXmppRequest, tracker: GCMRequestTracker) {
  def this(tuple2: (FCMXmppRequest, GCMRequestTracker)) = this(tuple2._1, tuple2._2)
}

class XmppNeverAckException(message: String) extends Exception(message) with NoStackTrace

class XmppNackException(val response: XmppNack) extends Exception(response.errorDescription) with NoStackTrace

class XmppConnectionPriorityMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(
  PriorityGenerator {
    case com.flipkart.connekt.commons.iomodels.XmppControl => 0
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.Shutdown => 1
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionActor.StartShuttingDown => 1
    case com.flipkart.connekt.busybees.xmpp.Internal.ReSize => 1
    case com.flipkart.connekt.commons.iomodels.XmppAck => 2
    case com.flipkart.connekt.commons.iomodels.XmppNack => 2
    case com.flipkart.connekt.commons.iomodels.XmppReceipt => 3
    case com.flipkart.connekt.commons.iomodels.XmppUpstreamData => 4
    case _ => 5
  }
)
