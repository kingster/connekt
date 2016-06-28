package com.flipkart.connekt.receptors.tests.routes.callbacks

import java.util.UUID

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Subscription
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.callbacks.SubscriptionsRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest
import org.apache.commons.lang.StringEscapeUtils
import com.flipkart.connekt.commons.utils.StringUtils._

class SubscriptionsRouteTest() extends BaseRouteTest {

  val subscriptionRoute = new SubscriptionsRoute().route
  override val header = RawHeader("x-api-key", "connekt-genesis")
  var subscription: Subscription = _
  implicit val mat = ActorMaterializer()

  var JSONRequest =
    s"""
       |{
       |	"name" : "SubscriptionRouteTest",
       |	"sink":
       |    {
       |        "type" : "HTTP",
       |        "method" : "POST",
       |        "url" : "http://requestb.in/wis41kwi"
       |    },
       |	"eventFilter": "testEventFilter",
       |  "eventTransformer":
       |    {
       |        "header" : "testHeader",
       |        "payload" : "testPayload"
       |    },
       |	"shutdownThreshold" : "4"
       |}
      """.stripMargin


  val failureJSONRequest =
    s"""
       |{
       |	"nameFail" : "SubscriptionRouteTest",
       |	"sinkFail":
       |    {
       |        "type" : "HTTP",
       |        "method" : "POST",
       |        "url" : "http://requestb.in/wis41kwi"
       |    },
       |	"eventFilter": "testEventFilter",
       |  "eventTransformer":
       |    {
       |        "header" : "testHeader",
       |        "payload" : "testPayload"
       |    },
       |	"shutdownThreshold" : "4"
       |}
      """.stripMargin


  val updatedJSONRequest =
    s"""
       |{
       |	"name" : "SubscriptionRouteTestUpdate",
       |	"sink":
       |    {
       |        "type" : "HTTP",
       |        "method" : "PUT",
       |        "url" : "http://requestb.in/wis41kwi"
       |    },
       |	"eventFilter": "updatedEventFilter",
       |  "eventTransformer":
       |    {
       |        "header" : "updatedHeader",
       |        "payload" : "updatedPayload"
       |    },
       |	"shutdownThreshold" : "6"
       |}
      """.stripMargin


  "Create Test" should "return Created" in {

    Post("/v1/subscription", HttpEntity(MediaTypes.`application/json`, JSONRequest)).addHeader(header) ~>
      subscriptionRoute ~> check {
      subscription = response.entity.getString(mat).getObj[GenericResponse].response.getJson.getObj[Response].data.asInstanceOf[Map[String, String]].getJson.getObj[Subscription]
      status shouldEqual StatusCodes.Created
    }
  }

  "Create Test" should "return Internal Server Error" in {

    Post("/v1/subscription", HttpEntity(MediaTypes.`application/json`, failureJSONRequest)).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.InternalServerError
    }
  }

  "Get Test" should "return OK" in {
    Get("/v1/subscription/" + subscription.id).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Start Test" should "return OK" in {
    Get("/v1/subscription/" + subscription.id + "/start").addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Start Test" should "return BadRequest" in {
    Get("/v1/subscription/" + UUID.randomUUID().toString + "/start").addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  "Stop Test" should "return OK" in {
    Get("/v1/subscription/" + subscription.id + "/stop").addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Stop Test" should "return BadRequest" in {
    Get("/v1/subscription/" + UUID.randomUUID().toString + "/stop").addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  "Update Test" should "return OK" in {
    Post("/v1/subscription/" + subscription.id, HttpEntity(MediaTypes.`application/json`, updatedJSONRequest)).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Update Test" should "return BadRequest" in {
    Post("/v1/subscription/" + UUID.randomUUID().toString, HttpEntity(MediaTypes.`application/json`, updatedJSONRequest)).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  "Delete Test" should "return OK" in {
    Delete("/v1/subscription/" + subscription.id).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "Delete Test" should "return BadRequest" in {
    Delete("/v1/subscription/" + UUID.randomUUID().toString).addHeader(header) ~>
      subscriptionRoute ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

}
