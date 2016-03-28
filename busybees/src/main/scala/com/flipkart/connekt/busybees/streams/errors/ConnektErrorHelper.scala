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
package com.flipkart.connekt.busybees.streams.errors

import com.flipkart.connekt.commons.iomodels.{ConnektRequest, PNRequestInfo}

object ConnektErrorHelper {

  abstract class FlatConnektRequest

  implicit class FlatPNRequest(request: ConnektRequest) extends FlatConnektRequest {
    val pnInfo = request.channelInfo.asInstanceOf[PNRequestInfo]

    def id = request.id
    def deviceId = Option(pnInfo).map(_.deviceId).orNull
    def appName = Option(pnInfo).map(_.appName).orNull
    def platform = Option(pnInfo).map(_.platform).orNull
  }
}
