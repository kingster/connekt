package com.flipkart.connekt.busybees.processors

import akka.actor.{Props, Actor}
import com.flipkart.connekt.commons.iomodels.{PNRequestData, ConnektRequest}

/**
 *
 *
 * @author durga.s
 * @version 12/5/15
 */
class PNProcessor extends Actor {
  lazy val androidPNProcessor = context.actorOf(Props[AndroidPNProcessor])
  lazy val windowsPNProcessor = context.actorOf(Props[WindowsPNProcessor])
  lazy val iosPNProcessor = context.actorOf(Props[IosPNProcessor])

  override def receive: Receive = {
    case connektRequest: ConnektRequest =>
      val pnRequestData = connektRequest.channelData.asInstanceOf[PNRequestData]
      pnRequestData.platform.toLowerCase match {
        case "android" => androidPNProcessor ! (connektRequest.id, pnRequestData)
        case "windows" => windowsPNProcessor ! (connektRequest.id, pnRequestData)
        case "ios" => iosPNProcessor ! (connektRequest.id, pnRequestData)
      }
  }
}
