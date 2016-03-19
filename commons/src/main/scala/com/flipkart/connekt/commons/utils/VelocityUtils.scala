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
package com.flipkart.connekt.commons.utils

import com.fasterxml.jackson.databind.node.{ArrayNode, NullNode, ObjectNode, ValueNode}
import org.apache.velocity.VelocityContext
import org.apache.velocity.context.Context

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object VelocityUtils {

  /**
   * Convert to Velocity Context Helpers
   * --------- START ------------------
   */
  def convertToVelocityContext(node: ObjectNode): Context = {
    val fields = node.fieldNames()
    val vContext = new VelocityContext()
    fields.asScala.foreach(fieldName =>  vContext.put(fieldName, getValue(node.get(fieldName))))
    vContext
  }

  private def convertToVelocityContext(array: ArrayNode): Any = {
    array.map(row =>  getValue(row)).asJava
  }


  private  def getValue(obj: Any): Any = {
    obj match {
      case _: ArrayNode => convertToVelocityContext(obj.asInstanceOf[ArrayNode])
      case _: ObjectNode => convertToVelocityContext(obj.asInstanceOf[ObjectNode])
      case  _: NullNode | null => null
      case _:ValueNode => obj.asInstanceOf[ValueNode].asText()
      case _ => obj.toString
    }
  }

  /**
   * --------- END ------------------
   */
}
