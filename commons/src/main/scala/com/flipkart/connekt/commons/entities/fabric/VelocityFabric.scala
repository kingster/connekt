/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities.fabric

import java.io.StringWriter

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.commons.utils.VelocityUtils
import org.apache.velocity.app.Velocity
import org.apache.velocity.context.Context

import scala.util.{Failure, Success, Try}

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

  def validateVtl(): Try[Boolean]
}

class EmailVelocityFabric(subjectVtl: String, bodyHtmlVtl: String) extends VelocityFabric with EmailFabric {
  override def validateVtl(): Try[Boolean] = Try.apply(true)

  override def getSubject(id: String, context: ObjectNode): String = {
    fabricate(id, context, subjectVtl, s"_$id _").get
  }

  override def getBodyHtml(id: String, context: ObjectNode): String = {
    fabricate(id, context, bodyHtmlVtl, s"_$id _").get
  }
}

class PNVelocityFabric(dataVtl: String) extends VelocityFabric with PNFabric {
  override def validateVtl(): Try[Boolean] = Try.apply(true)

  override def getData(id: String, context: ObjectNode): ObjectNode = {
    fabricate(id, context, dataVtl, s"_$id _").get.getObj[ObjectNode]
  }
}
