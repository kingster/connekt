package com.flipkart.connekt.commons.tests.helpers

import com.flipkart.connekt.commons.tests.CommonsBaseTest

class KafkaConsumerHelperTest  extends CommonsBaseTest {
  val topic = "push_8e494f43126ade96ce7320ad8ccfc709"


  "kafka offsets" should "not return null" in {
    val kafkaConsumerHelper = getKafkaConsumerHelper
    kafkaConsumerHelper.offsets(topic) should not be empty
  }


}
