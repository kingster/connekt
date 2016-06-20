package com.flipkart.connekt.commons.tests.services

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{HTTPRelayPoint, Subscription}
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import jdk.nashorn.internal.runtime.Specialization

/**
  * Created by harshit.sinha on 17/06/16.
  */

class SubscriptionServiceTest extends CommonsBaseTest {

  val subscription = new Subscription()

  subscription.name = "SubscriptionServiceTest"
  subscription.shutdownThreshold = 4
  subscription.createdBy = "connekt-insomnia"
  subscription.relayPoint = new HTTPRelayPoint("http://localhost:8080/serviceTestingRoute")
  subscription.groovyFilter = "This is groovyFilter string for SubscriptionServiceTest"

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
    subscription.groovyFilter = "This is a updated groovyFilter string for SubscriptionServiceTest"
    val sub = SubscriptionService.update(subscription)
    assert(sub.isSuccess)

  }

  "remove test" should "not throw exception" in {
    noException should be thrownBy SubscriptionService.remove(subscription.id)
  }



}
