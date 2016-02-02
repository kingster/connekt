package com.flipkart.connekt.busybees.streams.sources

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.helpers.KafkaConsumerHelper
import com.flipkart.connekt.commons.utils.StringUtils._
import kafka.serializer.{Decoder, DefaultDecoder}

import scala.reflect.ClassTag

/**
 *
 *
 * @author durga.s
 * @version 1/28/16
 */
class KafkaSource[V: ClassTag](kafkaConsumerHelper: KafkaConsumerHelper, topic: String) extends GraphStage[SourceShape[V]] {

  val out: Outlet[V] = Outlet("KafkaMessageSource.Out")

  def commitOffset(o: Long) = {}

  override def shape: SourceShape[V] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(out, new OutHandler {

      var cC = kafkaConsumerHelper.getConnector
      var itr = initIterator()

      def initIterator() = {
        ConnektLogger(LogFile.PROCESSORS).info("Kafka Iterator Init.")
        cC.createMessageStreams[Array[Byte], V](Map[String, Int](topic -> 1), new DefaultDecoder(), new MessageDecoder[V]()).get(topic) match {
          case Some(s) => s.head.iterator()
          case None => throw new RuntimeException(s"No KafkaStreams for topic: $topic")
        }
      }

      override def onPull(): Unit = try {
        val m = itr.next()
        ConnektLogger(LogFile.PROCESSORS).info(s"KafkaSource::OnPull:: ${m.message().toString}")
        commitOffset(m.offset) //TODO
        push(out, m.message())
      } catch {
        case e: Exception =>
          ConnektLogger(LogFile.PROCESSORS).error(s"Kafka iteration error: ${e.getMessage}", e)
          kafkaConsumerHelper.returnConnector(cC)
          cC = kafkaConsumerHelper.getConnector
          itr = initIterator()
      }
    })
  }
}

class MessageDecoder[T: ClassTag](implicit tag: ClassTag[T]) extends Decoder[T] {
  override def fromBytes(bytes: Array[Byte]): T = objMapper.readValue(bytes.getString, tag.runtimeClass).asInstanceOf[T]
}

