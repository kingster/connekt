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

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.commons.iomodels.{GcmXmppRequest, XmppNack}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.typesafe.config.Config
import org.jivesoftware.smack.tcp.XMPPTCPConnection

private [busybees] object XmppConnectionHelper {

  case object ReConnect
  case object ConnectionDraining
  case object FreeConnectionAvailable
  case object ConnectionBusy
  case object Shutdown
  case object StartShuttingDown
  case object XmppRequestAvailable
  case object FreeConnectionCount
  case class ConnectionClosed(connection: XMPPTCPConnection)
  case class XmppAckExpired(tracker: GCMRequestTracker)
  case class ReSize(count:Int)

  val GCM_NAMESPACE:String = "google:mobile:data"
  val GCM_ELEMENT_NAME:String = "gcm"

  val archivedConnections = new scala.collection.mutable.HashSet[XMPPTCPConnection]()

  lazy val xmppHost: String = ConnektConfig.get("fcm.ccs.fcmServer").getOrElse("fcm-xmpp.googleapis.com")
  lazy val xmppPort: Int = ConnektConfig.get("fcm.ccs.fcmPort").getOrElse("5235").toInt
}

case class XmppOutStreamRequest(request:GcmXmppRequest,tracker: GCMRequestTracker) {
  def this(tuple2: (GcmXmppRequest, GCMRequestTracker)) = this(tuple2._1, tuple2._2)
}

class XmppNeverAckException(message: String) extends Exception(message)

class XmppNackException(val response: XmppNack) extends Exception(response.errorDescription)


class XmppConnectionPriorityMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(
  PriorityGenerator {
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.ConnectionDraining => 0
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.ReConnect => 1
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.Shutdown => 1
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.StartShuttingDown => 1
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.ReSize => 1
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.FreeConnectionCount => 1
    case com.flipkart.connekt.commons.iomodels.XmppUpstreamData => 2
    case com.flipkart.connekt.commons.iomodels.XmppReceipt =>3
    case com.flipkart.connekt.commons.iomodels.XmppAck => 4
    case com.flipkart.connekt.commons.iomodels.XmppNack => 5
    case _ => 6
  }
)
