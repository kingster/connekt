package com.flipkart.connekt.busybees.processors

import akka.actor.{Props, Actor}
import com.flipkart.connekt.commons.iomodels.{PNRequestData, PNRequestInfo, ConnektRequest}

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
    case request: ConnektRequest =>
      val pnRequestInfo = request.channelInfo.asInstanceOf[PNRequestInfo]
      val pNRequestData = request.channelData.asInstanceOf[PNRequestData]

      pnRequestInfo.platform.toLowerCase match {
        case "android" => androidPNProcessor ! (request.id, pnRequestInfo, pNRequestData)
        case "windows" => windowsPNProcessor ! (request.id, pnRequestInfo, pNRequestData)
        case "ios" => iosPNProcessor ! (request.id, pnRequestInfo, pNRequestData)
      }
  }
}
