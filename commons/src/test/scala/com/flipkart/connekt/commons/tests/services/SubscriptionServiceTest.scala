package com.flipkart.connekt.commons.tests.services

import com.flipkart.connekt.commons.entities.{HTTPEventSink, Subscription}
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class SubscriptionServiceTest extends CommonsBaseTest {

  val subscription = new Subscription()

  subscription.name = "SubscriptionServiceTest"
  subscription.shutdownThreshold = 4
  subscription.createdBy = "connekt-insomnia"
  subscription.eventSink = new HTTPEventSink("POST", "http://localhost:8080/serviceTestingRoute")
  subscription.eventFilter = "This is groovy eventFilter string for SubscriptionServiceTest"

  "add Test" should "return success" in {
    val id = SubscriptionService.add(subscription)
    subscription.id = id.get
    assert(id.isSuccess)
  }

  "get Test" should "return a subscription" in {
    val sub = SubscriptionService.get(subscription.id)
    assert(sub.get.get.isInstanceOf[Subscription])

  }

  "Update test" should "return success" in {
    subscription.eventFilter = "This is a updated groovy eventFilter string for SubscriptionServiceTest"
    val sub = SubscriptionService.update(subscription)
    assert(sub.isSuccess)

  }

  "remove test" should "not throw exception" in {
    noException should be thrownBy SubscriptionService.remove(subscription.id)
  }


}
