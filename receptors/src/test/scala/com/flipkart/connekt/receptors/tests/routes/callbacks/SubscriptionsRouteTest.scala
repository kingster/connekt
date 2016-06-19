package com.flipkart.connekt.receptors.tests.routes.callbacks

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import com.flipkart.connekt.commons.entities.Subscription
import com.flipkart.connekt.receptors.routes.callbacks.SubscriptionsRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest
import org.apache.commons.lang.StringEscapeUtils
import com.flipkart.connekt.commons.utils.StringUtils._

/**
  * Created by harshit.sinha on 08/06/16.
  */

class SubscriptionsRouteTest extends BaseRouteTest {

  val subscriptionRoute = new SubscriptionsRoute().route
  override val header = RawHeader("x-api-key", "b0979afd-2ce3-4786-af62-ab53f88204ae")
  var subscription: Subscription = _

  "Create Test" should "return OK" in {
    val groovyString =
      """
        |package com.flipkart.connekt.commons.entities;
        |import com.flipkart.connekt.commons.iomodels.CallbackEvent
        |import com.flipkart.connekt.commons.iomodels.PNCallbackEvent;
        |class ConnektSampleAppGroovy implements Evaluator {
        |public boolean evaluate(CallbackEvent context) {
        |return (context as PNCallbackEvent).eventType().equals("gcm_received")
        |}
        |
        |}
      """.stripMargin

    val JSONRequest =
      s"""
         |{
         |	"sName" : "SubscriptionRouteTest",
         |	"endpoint":
         |    {
         |        "type" : "HTTP",
         |        "url" : "http://localhost:8080/SubscriptionRouteTesting"
         |    },
         |	"groovyString": "${StringEscapeUtils.escapeJava(groovyString)}",
         |	"shutdownThreshold" : "4"
         |}
      """.stripMargin

    Post("/v1/subscription", HttpEntity(MediaTypes.`application/json`, JSONRequest)).addHeader(header) ~>
    subscriptionRoute ~> check {
      subscription = HttpResponse.getJson.getObj
      status shouldEqual StatusCodes.OK
    }
  }

  "Get Test" should "return OK" in {
    Get("/v1/subscription/"+subscription.id).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Start Test" should "return OK" in {
    Get("/v1/subscription/"+subscription.id).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Stop Test" should "return OK" in {
    Get("/v1/subscription/"+subscription.id).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Update Test" should "return OK" in {
    val groovyString =
      """
        |ackage com.flipkart.connekt.commons.entities;
        |import com.flipkart.connekt.commons.iomodels.CallbackEvent
        |import com.flipkart.connekt.commons.iomodels.PNCallbackEvent;
        |class ConnektSampleAppGroovy implements Evaluator {
        |public boolean evaluate(CallbackEvent context) {
        |return (context as PNCallbackEvent).eventType().equals("gcm_received")
        |}
        |
        |}
      """.stripMargin

    val payload =
      s"""
         |{
         |	"sName" : "SubscriptionRouteUpdateTest",
         |	"endpoint":
         |    {
         |        "type" : "HTTP",
         |        "url" : "http://localhost:8080/SubscriptionRouteUpdateTesting"
         |    },
         |	"groovyString": "${StringEscapeUtils.escapeJava(groovyString)}",
         |	"shutdownThreshold" : "2"
         |}
      """.stripMargin

    Post("/v1/subscription/"+subscription.id, HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Delete Test" should "return OK" in {
    Delete("/v1/subscription/"+subscription.id).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

}
