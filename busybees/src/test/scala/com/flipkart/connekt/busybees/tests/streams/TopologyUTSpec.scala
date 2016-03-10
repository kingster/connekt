package com.flipkart.connekt.busybees.tests.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * Created by kinshuk.bairagi on 05/02/16.
 */
class TopologyUTSpec extends CommonsBaseTest {

  implicit val system = ActorSystem("Test")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()

}
