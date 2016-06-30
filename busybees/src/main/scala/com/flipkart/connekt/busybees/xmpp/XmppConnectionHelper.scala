package com.flipkart.connekt.busybees.xmpp

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.flipkart.connekt.busybees.models.GCMRequestTracker
import com.flipkart.connekt.commons.entities.DeviceDetails
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, XmppNack}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.utils.StringUtils
import com.typesafe.config.Config
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnection

/**
 * Created by subir.dey on 28/06/16.
 */
object XmppConnectionHelper {
  case object InitXmpp
  case object ReConnect
  case object ConnectionDraining
  case object FreeConnectionAvailable
  case object XmppRequestAvailable
  case class ConnectionClosed(connection: XMPPTCPConnection)
  case class XmppAckExpired(tracker: GCMRequestTracker)

  val GCM_NAMESPACE:String = "google:mobile:data"
  val GCM_ELEMENT_NAME:String = "gcm"

  val archivedConnections: java.util.Set[XMPPConnection] = new java.util.HashSet[XMPPConnection]()

  lazy val xmppHost: String = ConnektConfig.get("gcm.ccs.gcmServer").getOrElse("fcm-xmpp.googleapis.com")
  lazy val xmppPort: Int = ConnektConfig.get("gcm.ccs.gcmPort").getOrElse("5235").toInt
}

class XmppNeverAckException(message: String) extends Exception(message)

class XmppNackException(val response: XmppNack) extends Exception(response.errorDescription)


class XmppConnectionPriorityMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(
  PriorityGenerator {
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.ConnectionDraining => 0
    case com.flipkart.connekt.busybees.xmpp.XmppConnectionHelper.ReConnect => 1
    case com.flipkart.connekt.commons.iomodels.XmppUpstreamData => 2
    case com.flipkart.connekt.commons.iomodels.XmppReceipt =>3
    case com.flipkart.connekt.commons.iomodels.XmppAck => 4
    case com.flipkart.connekt.commons.iomodels.XmppNack => 5
    case _ => 6
  }
)

import com.flipkart.connekt.commons.utils.StringUtils._

object XmppMessageIdHelper {
  def generateMessageId(message: ConnektRequest, device:DeviceDetails):String = message.id + ":" + device.deviceId + ":" + message.contextId.orEmpty

  def parseMessageIdTo(messageStr:String):(String,String) = {
    val splitString = messageStr.split(':')
    val originalMessageId = if (StringUtils.isNullOrEmpty(messageStr)) "" else splitString(0)
    val context = if ( splitString.length >= 3) splitString(2) else ""
    (originalMessageId,context)
  }
}


