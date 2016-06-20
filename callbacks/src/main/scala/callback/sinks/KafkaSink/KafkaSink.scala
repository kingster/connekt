package callback.sinks.KafkaSink

import akka.stream.scaladsl.Sink

/**
  * Created by harshit.sinha on 16/06/16.
  */
class KafkaSink(brokers:String, topic:String) {
  def getKafkaSink = Sink.foreach(println)
}
