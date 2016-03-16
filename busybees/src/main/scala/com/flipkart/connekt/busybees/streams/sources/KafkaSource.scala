package com.flipkart.connekt.busybees.streams.sources

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, TimerGraphStageLogic}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.metrics.Instrumented
import com.flipkart.connekt.commons.utils.CollectionUtils._
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.metrics.Timed
import kafka.consumer.{ConsumerConnector, ConsumerTimeoutException}
import kafka.message.MessageAndMetadata
import kafka.serializer.{Decoder, DefaultDecoder}
import kafka.utils.ZKStringSerializer
import org.I0Itec.zkclient.ZkClient

import scala.collection.Iterator
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 1/28/16
 */
class KafkaSource[V: ClassTag](kafkaConsumerHelper: KafkaConsumerHelper, topic: String)(shutdownTrigger: Future[String])(implicit val ec: ExecutionContext) extends GraphStage[SourceShape[V]] with Instrumented{

  val out: Outlet[V] = Outlet("KafkaMessageSource.Out")

  def commitOffset(o: Long) = {}

  override def shape: SourceShape[V] = SourceShape(out)

  lazy val zk = new ZkClient(kafkaConsumerHelper.zkPath(), 5000, 5000, ZKStringSerializer)

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
        val m = iterator.next()
        commitOffset(m.offset)
        push(out, m.message())
      } else {
        ConnektLogger(LogFile.PROCESSORS).warn(s"KafkaSource:: pushElement no-data")
        scheduleOnce(TimerPollTrigger, timerDelayInMs)
      }

      def safeHasNext = try { iterator.hasNext } catch { case e: ConsumerTimeoutException => false }
    }

    setHandler(out, new OutHandler {
      override def onPull(): Unit = try {
        pushElement()
      } catch {
        case e: Exception =>
          ConnektLogger(LogFile.PROCESSORS).error(s"KafkaSource:: iteration error: ${e.getMessage}", e)
          kafkaConsumerHelper.returnConnector(kafkaConsumerConnector)
          createKafkaConsumer()
        /*failStage(e)*/
      }
    })

    override def preStart(): Unit = {
      createKafkaConsumer()
      val handle = getAsyncCallback[String] { (r: String) => completeStage()}

      shutdownTrigger onComplete { t =>
        ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource $topic async shutdown trigger invoked.")
        handle.invoke(t.getOrElse("_external topology shutdown signal_"))
      }

      super.preStart()
    }
  }

  /* KAFKA Operations */
  var kafkaConsumerConnector: ConsumerConnector = null
  var iterator: Iterator[MessageAndMetadata[Array[Byte], V]] = Iterator.empty

  private def initIterator(kafkaConnector: ConsumerConnector): Iterator[MessageAndMetadata[Array[Byte], V]] = {

    ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource Init Topic[$topic], Streams[1]")

    /**
      * Using threadCount = 1, since for now we got the best performance with this.
      * Once akka/reactive-kafka get's stable, we will move to it provided it gives better performance.
      */
    val consumerStreams = kafkaConnector.createMessageStreams[Array[Byte], V](Map[String, Int](topic -> 1), new DefaultDecoder(), new MessageDecoder[V]())
    val streams = consumerStreams.getOrElse(topic,throw new Exception(s"No KafkaStreams for topic: $topic"))
    Try(streams.map(_.iterator()).head).getOrElse {
      ConnektLogger(LogFile.PROCESSORS).warn(s"KafkaSource:: stream could not be created for $topic")
      Iterator.empty
    }
  }

  private def createKafkaConsumer(): Unit = {
    ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource::createKafkaConsumer")

    kafkaConsumerConnector = kafkaConsumerHelper.getConnector
    iterator = initIterator(kafkaConsumerConnector)
    ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource::initIterator Complete")
  }
}

class MessageDecoder[T: ClassTag](implicit tag: ClassTag[T]) extends Decoder[T] {
  override def fromBytes(bytes: Array[Byte]): T = objMapper.readValue(bytes.getString, tag.runtimeClass).asInstanceOf[T]
}


