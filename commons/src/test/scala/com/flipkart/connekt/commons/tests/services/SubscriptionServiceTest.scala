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
package com.flipkart.connekt.commons.tests.services

import java.util.UUID

import com.flipkart.connekt.commons.entities.{HTTPEventSink, Subscription, Transformers}
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class SubscriptionServiceTest extends CommonsBaseTest {

  val subscription = new Subscription()

  subscription.name = "SubscriptionServiceTest"
  subscription.shutdownThreshold = 4
  subscription.createdBy = "connekt-insomnia"
  subscription.sink = new HTTPEventSink("POST", "http://requestb.in/wis41kwi")
  subscription.eventFilter = "testEventFilter"
  subscription.eventTransformer = new Transformers("testHeader","testPayload")

  "add Test" should "return success" in {
    val id = SubscriptionService.add(subscription)
    subscription.id = id.get
    assert(id.isSuccess)
  }

  "get Test" should "return a subscription" in {
    assert(SubscriptionService.get(subscription.id).get.get.isInstanceOf[Subscription])

  }

  "get Test" should "not return a subscription" in {
    assert(SubscriptionService.get(UUID.randomUUID().toString).get.isEmpty)
  }

  "update Test" should "return success" in {
    subscription.eventFilter = "updatedEventFilter"
    assert(SubscriptionService.update(subscription).isSuccess)

  }

  "update Test" should "return failure" in {
    val id = subscription.id
    subscription.id = UUID.randomUUID().toString
    assert(SubscriptionService.update(subscription).isFailure)
    subscription.id = id
  }

  "remove test" should "not throw exception" in {
    noException should be thrownBy SubscriptionService.remove(subscription.id)
  }

  "remove Test" should "return failure" in {
    assert(SubscriptionService.remove(UUID.randomUUID().toString).isFailure)
  }


}
