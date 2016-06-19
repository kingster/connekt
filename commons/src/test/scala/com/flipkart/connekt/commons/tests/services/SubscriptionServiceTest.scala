package com.flipkart.connekt.commons.tests.services

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{HTTPRelayPoint, Subscription}
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
  * Created by harshit.sinha on 17/06/16.
  */
class SubscriptionServiceTest extends CommonsBaseTest{

  val subscription = new Subscription

  subscription.name = "SubscriptionServiceTest"
  subscription.id = UUID.randomUUID().toString
  subscription.shutdownThreshold = 4
  subscription.createdBy = "connekt-insomnia"
  subscription.relayPoint = new HTTPRelayPoint("http://localhost:8080/serviceTestingRoute")
  subscription.groovyFilter = "This is groovyFilter string for SubscriptionServiceTest"


  "add Test" should "add a subscription and return success" in {
    val assertion = SubscriptionService.add(subscription)
    assert(assertion.isSuccess && assertion.isInstanceOf[Subscription])
  }

  "get Test" should "return a subscription" in {
    assert(SubscriptionService.get(subscription.id).get.isInstanceOf[Subscription])
  }

  "Update test" should "update subscription and return success" in {
    subscription.groovyFilter = "This is a updated groovyFilter string for SubscriptionServiceTest"
    val assertion = SubscriptionService.update(subscription, subscription.id)
    assert(assertion.isSuccess && assertion.isInstanceOf[Subscription])
  }

  "remove test" should "not throw exception" in {
    noException should be thrownBy SubscriptionService.remove(subscription.id)
  }


}
