package com.flipkart.connekt.commons.entities.fabric

import java.io.StringWriter

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import org.apache.velocity.app.Velocity
import org.apache.velocity.context.Context

import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
@JsonTypeInfo(
use = JsonTypeInfo.Id.NAME,
include = JsonTypeInfo.As.PROPERTY,
property = "cType"
)
@JsonSubTypes(Array(
new Type(value = classOf[PNVelocityFabric], name = "PN"),
new Type(value = classOf[EmailVelocityFabric], name = "EMAIL")
))
sealed abstract class VelocityFabric extends EngineFabric {
  /**
   *
   * @param context velocity engine operation context
   * @param vtlFabric input string containing the VTL to be rendered
   * @param errorTag identifier stencil name for log messages in case of error
   * @return output string of velocity rendering
   */
  def fabricate(context: Context, vtlFabric: String, errorTag: String): String = {
    val w = new StringWriter()
    Velocity.evaluate(context, w, errorTag, vtlFabric)
    w.toString
  }

  def validateVtl(): Try[Boolean]
}

abstract class EmailVelocityFabric(subjectVtl: String, bodyHtmlVtl: String) extends VelocityFabric with EmailFabric

abstract class PNVelocityFabric(dataVtl: String) extends VelocityFabric with PNFabric