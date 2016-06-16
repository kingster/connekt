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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.busybees.streams.errors.ConnektPNStageException
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels.{ChannelRequestData, ConnektRequest}
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils._

class RenderFlow extends MapFlowStage[ConnektRequest, ConnektRequest] {

  override val map: (ConnektRequest) => List[ConnektRequest] = input => {
    try {
      ConnektLogger(LogFile.PROCESSORS).debug(s"RenderFlow received message: ${input.id}")
      ConnektLogger(LogFile.PROCESSORS).trace(s"RenderFlow received message: ${input.getJson}")
      val stencils = input.templateId.flatMap(StencilService.get(_)).getOrElse(List.empty)

      val mRendered = input.copy(channelData = Option(input.channelData) match {
        case Some(cD) => cD
        case None =>
          (Channel.withName(input.channel) match {
            case Channel.PUSH =>
              (stencils.map(s => s.component -> StencilService.render(s, input.channelDataModel).getObj[ObjectNode]) ++ Map("type" -> "PN")).toMap
            case _ =>
              (stencils.map(s => s.component -> StencilService.render(s, input.channelDataModel)) ++ Map("type" -> input.channel)).toMap
          }).getJson.getObj[ChannelRequestData]
      }, meta = input.meta ++ input.templateId.map("stencilId" -> _).toMap)

      List(mRendered)
    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"RenderFlow error", e)
        throw new ConnektPNStageException(input.id, input.clientId,input.deviceId, InternalStatus.RenderFailure, input.appName, input.platform, input.contextId.orEmpty,  input.meta ++ input.templateId.map("stencilId" -> _).toMap, s"RenderFlow-${e.getMessage}", e)
    }
  }
}
