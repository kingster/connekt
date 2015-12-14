package com.flipkart.connekt.commons.utils

import com.fasterxml.jackson.databind.node.{NullNode, ArrayNode, ObjectNode}
import org.apache.velocity.VelocityContext
import org.apache.velocity.context.Context

import scala.collection.mutable.ListBuffer

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
object VelocityUtils {
  def convert2VelocityContext(node: ObjectNode): Context = {
    val fields = node.fieldNames()
    val vContext = new VelocityContext()

    while(fields.hasNext) {
      val k = fields.next()
      Option(getValue(node.get(k))).foreach({vContext.put(k, _)})
    }

    vContext
  }

  private def convert2VelocityContext(array: ArrayNode): AnyRef = {
    val values = new ListBuffer[AnyRef]()
    val i = array.elements()
    while(i.hasNext)
      Option(getValue(i.next())).foreach({values += _})

    values.toList
  }

  private def getValue(x: AnyRef): AnyRef = {
    x match {
      case _: ObjectNode =>
        convert2VelocityContext(x.asInstanceOf[ObjectNode])
      case _: ArrayNode =>
        convert2VelocityContext(x.asInstanceOf[ArrayNode])
      case _: NullNode =>
        null
    }
    x.toString
  }
}
