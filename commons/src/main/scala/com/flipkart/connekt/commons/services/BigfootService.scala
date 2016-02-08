package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils._
import com.flipkart.phantom.client.exceptions.PhantomClientException
import com.flipkart.seraph.schema.BaseSchema
import com.flipkart.specter.ingestion.IngestionMetadata
import com.flipkart.specter.ingestion.events.Event
import com.flipkart.specter.{SpecterClient, SpecterRequest}

import scala.util.{Failure, Success, Try}

/**
 * Created by nidhi.mehla on 02/02/16.
 */
object BigfootService {

  val socketClient = DaoFactory.phantomClientSocket

  def ingest(obj: BaseSchema): Try[Boolean] = {

    val eventId = StringUtils.generateRandomStr(25)
    val eventTime = System.currentTimeMillis()
    val event = new Event(eventId, eventTime.toString, obj)
    val ingestionMetadata: IngestionMetadata = new IngestionMetadata()
    ingestionMetadata.setRequestId(eventId)
    val request: SpecterRequest = new SpecterRequest(event, ingestionMetadata)

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
        ConnektLogger(LogFile.SERVICE).error("Specter connection Error, FAILED_TO_INGEST", e)
        Failure(e)
      case ie: InterruptedException =>
        ConnektLogger(LogFile.SERVICE).error("Interrupt Exception , FAILED_TO_INGEST", ie)
        Failure(ie)
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("Unknown ERROR, FAILED_TO_INGEST", e)
        Failure(e)
    }
  }
}
