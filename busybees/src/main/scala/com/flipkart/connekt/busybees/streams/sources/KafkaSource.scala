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
package com.flipkart.connekt.busybees.streams.sources

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, TimerGraphStageLogic}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaConnectionHelper
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.metrics.Timed
import com.typesafe.config.Config
import kafka.consumer.{ConsumerConnector, ConsumerTimeoutException}
import kafka.message.MessageAndMetadata
import kafka.serializer.{Decoder, DefaultDecoder}

import scala.collection.Iterator
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

class KafkaSource[V: ClassTag](kafkaConsumerConf: Config, topic: String, groupId: String)(shutdownTrigger: Future[String])(implicit val ec: ExecutionContext) extends GraphStage[SourceShape[V]] with KafkaConnectionHelper with Instrumented {

  val out: Outlet[V] = Outlet("KafkaMessageSource.Out")

  def commitOffset(o: Long) = {}

  override def shape: SourceShape[V] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {

    case object TimerPollTrigger

    val timerDelayInMs = 100.milliseconds

    override protected def onTimer(timerKey: Any): Unit = {
      if (timerKey == TimerPollTrigger)
        pushElement()
    }

    @Timed("pushElement")
    private def pushElement() = {
      if (safeHasNext) {
        var n: MessageAndMetadata[Array[Byte], Option[V]] = null
        val retries = new AtomicInteger(0)

        do {
          n = iterator.next()
        } while (n.message().isEmpty && safeHasNext && retries.getAndIncrement < 1000)

        n.message() match {
          case Some(m) =>
            ConnektLogger(LogFile.PROCESSORS).info(s"message from topic:${n.topic}, partition: ${n.partition} and offset: ${n.offset}")
            commitOffset(n.offset)
            push(out, m)
          case None =>
            ConnektLogger(LogFile.PROCESSORS).warn(s"KafkaSource no valid data in 1000 retries.")
            scheduleOnce(TimerPollTrigger, timerDelayInMs)
        }
      } else {
        //ConnektLogger(LogFile.PROCESSORS).trace(s"KafkaSource pushElement no-data")
        scheduleOnce(TimerPollTrigger, timerDelayInMs)
      }

      def safeHasNext = try { iterator.hasNext } catch { case e: ConsumerTimeoutException => false }
    }

    setHandler(out, new OutHandler {
      override def onPull(): Unit = try {
        pushElement()
      } catch {
        case e: Exception =>
          ConnektLogger(LogFile.PROCESSORS).error(s"KafkaSource iteration error: ${e.getMessage}", e)
          kafkaConsumerConnector.shutdown()
          ConnektLogger(LogFile.PROCESSORS).info(s"Shutting down kafka consumer connector post iteration error.")
          initKafkaConsumer()
      }
    })


    override def preStart(): Unit = {
      initKafkaConsumer()
      val startOffset = offsets(topic, groupId, zkPath(kafkaConsumerConf))
      ConnektLogger(LogFile.PROCESSORS).info(s"kafkaOffsets and owner on Start for topic $topic are: ${startOffset.toString()}")

      val handle = getAsyncCallback[String] { (r: String) => completeStage()}

      shutdownTrigger onComplete { t =>
        ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource $topic async shutdown trigger invoked.")
        handle.invoke(t.getOrElse("_external topology shutdown signal_"))
        val stopOffsets = offsets(topic, groupId, zkPath(kafkaConsumerConf))
        ConnektLogger(LogFile.PROCESSORS).info(s"kafkaOffsets and owner on Stop for topic $topic are: ${stopOffsets.toString()}")
        kafkaConsumerConnector.shutdown()
      }

      super.preStart()
    }
  }

  /* KAFKA Operations */
  var kafkaConsumerConnector: ConsumerConnector = null
  var iterator: Iterator[MessageAndMetadata[Array[Byte], Option[V]]] = Iterator.empty

  private def initIterator(kafkaConnector: ConsumerConnector): Iterator[MessageAndMetadata[Array[Byte], Option[V]]] = {

    ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource Init Topic[$topic], Streams[1]")

    /**
      * Using threadCount = 1, since for now we got the best performance with this.
      * Once akka/reactive-kafka get's stable, we will move to it provided it gives better performance.
      */
    val consumerStreams = kafkaConnector.createMessageStreams(Map[String, Int](topic -> 1), new DefaultDecoder(), new MessageDecoder[V]())
    val streams = consumerStreams.getOrElse(topic,throw new Exception(s"No KafkaStreams for topic: $topic"))
    Try(streams.map(_.iterator()).head).getOrElse {
      ConnektLogger(LogFile.PROCESSORS).warn(s"KafkaSource stream could not be created for $topic")
      Iterator.empty
    }
  }

  private def initKafkaConsumer(): Unit = {
    ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource create kafka consumer")

    kafkaConsumerConnector = createKafkaConsumer(groupId, kafkaConsumerConf)
    iterator = initIterator(kafkaConsumerConnector)
    ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource init iterator complete")
  }
}

class MessageDecoder[T: ClassTag](implicit tag: ClassTag[T]) extends Decoder[Option[T]] {
  override def fromBytes(bytes: Array[Byte]): Option[T] = try {
    Option(objMapper.readValue(bytes.getString, tag.runtimeClass).asInstanceOf[T])
  } catch {
    case e: Exception =>
      ConnektLogger(LogFile.PROCESSORS).error(s"KafkaSource de-serialization failure, ${e.getMessage}", e)
      None
  }
}
