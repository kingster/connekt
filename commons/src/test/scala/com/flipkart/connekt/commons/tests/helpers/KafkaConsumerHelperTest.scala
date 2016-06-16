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
package com.flipkart.connekt.commons.tests.helpers


import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.services.CallbackService
import com.flipkart.connekt.commons.utils.StringUtils

class KafkaConsumerHelperTest  extends CommonsBaseTest {
  val topic = "push_8e494f43126ade96ce7320ad8ccfc709"

  val kafkaHelper = getKafkaConsumerHelper

  "kafka offsets" should "not return null" in {
    kafkaHelper.offsets(topic) should not be empty

  }

  "read some messages from kafka topic" should "process some messages" in {
     kafkaHelper.getConnector.createMessageStreams(Map("active_events" -> 1)).foreach(x => {
       x._2.foreach(y => {
         y.foreach(data => {
           println(data.message())
         })
       })
     })


  }

  "return consumer connector" should "return the connector to the pool" in {
  }

  "get connector" should "return a working connector" in {
    //read some messages
  }
}
