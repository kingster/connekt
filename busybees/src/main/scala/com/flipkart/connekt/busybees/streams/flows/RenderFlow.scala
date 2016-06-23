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
package com.flipkart.connekt.busybees.streams.flows

import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.commons.factories.{ServiceFactory, ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.utils.StringUtils._

class RenderFlow extends MapFlowStage[ConnektRequest, ConnektRequest] {

  implicit val stencilService = ServiceFactory.getStencilService

  override val map: (ConnektRequest) => List[ConnektRequest] = input => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"RenderFlow received message: ${input.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"RenderFlow received message: ${input.getJson}")

      val mRendered = input.copy(channelData = Option(input.channelData) match {
        case Some(cD) => cD
        case None => input.getComputedChannelData
      }, meta = input.meta ++ input.stencilId.map("stencilId" -> _).toMap)

      List(mRendered)
    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"RenderFlow error", e)
        throw new ConnektPNStageException(input.id, input.clientId,input.deviceId, InternalStatus.RenderFailure, input.appName, input.platform, input.contextId.orEmpty,  input.meta ++ input.stencilId.map("stencilId" -> _).toMap, s"RenderFlow-${e.getMessage}", e)
    }
  }
}
