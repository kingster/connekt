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
package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.bigfoot.{EntityBaseSchema, EventBaseSchema}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils._
import com.flipkart.metrics.Timed
import com.flipkart.phantom.client.exceptions.PhantomClientException
import com.flipkart.seraph.schema.BaseSchema
import com.flipkart.specter.ingestion.IngestionMetadata
import com.flipkart.specter.ingestion.entities.Entity
import com.flipkart.specter.ingestion.events.Event
import com.flipkart.specter.{SpecterClient, SpecterRequest}
import com.flipkart.connekt.commons.utils.StringUtils._
import scala.util.{Failure, Success, Try}

object BigfootService extends Instrumented {

  val socketClient = DaoFactory.phantomClientSocket

  val ingestionEnabled = ConnektConfig.getBoolean("flags.bf.enabled").getOrElse(false)

  private def ingest(request: SpecterRequest): Try[Boolean] = {
    if (ingestionEnabled) {
      try {
        SpecterClient.sendToSpecter(socketClient, request).getResponse match {
          case "SUCCESS" =>
            Success(true)
          case commandResponse: String =>
            ConnektLogger(LogFile.SERVICE).error(s"UnSuccessful Ingestion, FAILED_TO_INGEST : $commandResponse")
            Failure(new Exception(s"Unsuccessful specter response: $commandResponse"))
        }
      } catch {
        case e: PhantomClientException =>
          ConnektLogger(LogFile.SERVICE).error(s"Specter connection Error, FAILED_TO_INGEST [${request.getObject.getJson}]", e)
          Failure(e)
        case ie: InterruptedException =>
          ConnektLogger(LogFile.SERVICE).error(s"Interrupt Exception , FAILED_TO_INGEST [${request.getObject.getJson}]", ie)
          Failure(ie)
        case e: Exception =>
          ConnektLogger(LogFile.SERVICE).error(s"Unknown ERROR, FAILED_TO_INGEST [${request.getObject.getJson}]", e)
          Failure(e)
      }
    } else {
      // Ingestion disabled.
      ConnektLogger(LogFile.SERVICE).warn(s"BF_SKIP_INGEST ${request.getObject.getJson}")
      Success(true)
    }
  }

  @Timed("ingest")
  def ingest(obj: EventBaseSchema): Try[Boolean] = {
    val eventId = StringUtils.generateRandomStr(25)
    val event = new Event(eventId, System.currentTimeMillis(), obj)
    val ingestionMetadata: IngestionMetadata = new IngestionMetadata()
    ingestionMetadata.setRequestId(eventId)
    ingest(new SpecterRequest(event, ingestionMetadata))
  }

  @Timed("ingest")
  def ingest(obj: EntityBaseSchema): Try[Boolean] = {
    val entity = new Entity(obj.id, System.currentTimeMillis(), obj)
    val ingestionMetadata: IngestionMetadata = new IngestionMetadata()
    ingestionMetadata.setRequestId(obj.id)
    ingest(new SpecterRequest(entity, ingestionMetadata))
  }
}
