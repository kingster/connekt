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
package com.flipkart.connekt.commons.entities.fabric

import java.io.StringWriter

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.VelocityUtils
import org.apache.velocity.app.Velocity
import org.apache.velocity.context.Context

import scala.util.{Failure, Success, Try}

class VelocityFabric(dataVtl: String) extends EngineFabric {
  /**
   *
   * @param context velocity engine operation context
   * @param vtlFabric input string containing the VTL to be rendered
   * @param errorTag identifier stencil name for log messages in case of error
   * @return output string of velocity rendering
   */
  def fabricate(id: String, context: Context, vtlFabric: String, errorTag: String): Try[String] = {
    try {
      val w = new StringWriter()
      Velocity.evaluate(context, w, errorTag, vtlFabric)
      Success(w.toString)
    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.PROCESSORS).error(s"Velocity fabricate failed for [$id}], ${e.getMessage}", e)
        Failure(new Throwable(s"Velocity fabricate failed for [$id}] error: ${e.getMessage}"))
    }
  }

  def fabricate(id: String, context: ObjectNode, vtlFabric: String, errorTag: String): Try[String] = {
    fabricate(id, VelocityUtils.convertToVelocityContext(context), vtlFabric, errorTag)
  }

  def validateVtl(): Try[Boolean] = Try.apply(true)

  def renderData(id: String, context: ObjectNode): String = {
    fabricate(id, context, dataVtl, s"_$id _").get
  }
}
