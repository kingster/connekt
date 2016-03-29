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
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.ConnektRequestHelper._
import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils._

class RenderFlow extends MapFlowStage[ConnektRequest, ConnektRequest] {

  override val map: (ConnektRequest) => List[ConnektRequest] = input => {
    try {
      ConnektLogger(LogFile.PROCESSORS).info(s"RenderFlow:: onPush:: Received Message: ${input.getJson}")
      lazy val cRD = input.templateId.flatMap(StencilService.get(_)).map(StencilService.render(_, input.channelDataModel)).get

      val mRendered = input.copy(channelData = Option(input.channelData) match {
        case Some(cD) => cD
        case None => cRD
      })

      List(mRendered)
    } catch {
      case e: Throwable =>
        ConnektLogger(LogFile.PROCESSORS).error(s"RenderFlow:: onPush :: Error", e)
        throw new ConnektPNStageException(input.id, input.deviceId, "connekt_render_failure", input.appName, input.platform, "", e.getMessage, e)
    }
  }
}
