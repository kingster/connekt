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

import com.flipkart.concord.publisher.{RequestType, TPublishRequest, TPublishRequestMetadata, TPublisher}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils._
import com.flipkart.metrics.Timed

import scala.util.{Try, Failure, Success}

object BigfootService extends Instrumented {

  lazy val phantomSocketPath: String = ConnektConfig.getString("connections.specter.socket").get
  lazy val ingestionEnabled = ConnektConfig.getBoolean("flags.bf.enabled").getOrElse(false)

  lazy val phantomPublisher: TPublisher[String] = {
    if(ingestionEnabled) {
      try {
        val configLoaderClass = Class.forName("com.flipkart.connekt.util.mapper.PhantomSocketPublisher")
        configLoaderClass.getConstructor(classOf[String], classOf[String]).newInstance(phantomSocketPath, "publishToBigFoot").asInstanceOf[TPublisher[String]]
      } catch {
        case e: Exception =>
          ConnektLogger(LogFile.SERVICE).error("Unable to initialize PhantomPublisher", e)
          null
      }
    } else null
  }

  val socketClient = DaoFactory.phantomClientSocket

  private def ingest(request: TPublishRequest, requestMetadata: TPublishRequestMetadata): Try[Boolean] = {
    if(ingestionEnabled) {
      phantomPublisher.publish(request, requestMetadata).response match {
        case Success(m) if m.equalsIgnoreCase("SUCCESS") => Success(true)
        case Success(m) =>
          ConnektLogger(LogFile.SERVICE).error(s"Phantom ingestion failed. CommandResponse: $m")
          Failure(new Throwable(s"Phantom ingestion failed. CommandResponse: $m"))
        case Failure(t) => Failure(t)
      }
    } else {
      Success(true)
    }
  }

  @Timed("ingestEntity")
  def ingestEntity(entityId: String, request: TPublishRequest, entityNamespace: String) = ingest(request, new TPublishRequestMetadata {
    override def requestType: RequestType.Value = RequestType.Entity

    override def id: String = entityId

    override def namespace(): Option[String] = Some(entityNamespace)
  })

  @Timed("ingestEvent")
  def ingestEvent(request: TPublishRequest, eventNamespace: String) = ingest(request, new TPublishRequestMetadata {
    override def requestType: RequestType.Value = RequestType.Event

    override def id: String = StringUtils.generateRandomStr(25)

    override def namespace(): Option[String] = Some(eventNamespace)
  })
}
