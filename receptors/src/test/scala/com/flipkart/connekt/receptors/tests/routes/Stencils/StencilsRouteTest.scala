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
package com.flipkart.connekt.receptors.tests.routes.Stencils

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.{Stencil, StencilsEnsemble, StencilEngine}
import com.flipkart.connekt.commons.iomodels.Response
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest
import org.apache.commons.lang.StringEscapeUtils

import scala.concurrent.Await
import scala.concurrent.duration._

class StencilsRouteTest extends BaseRouteTest {
  val engine = StencilEngine.GROOVY
  val engineFabric =
    """
      |package com.flipkart.connekt.commons.entities.fabric;
      |
      |import com.fasterxml.jackson.databind.node.JsonNodeFactory;
      |import com.fasterxml.jackson.databind.node.ObjectNode;
      |
      |public class FlipkartRetailAppGroovy implements GroovyFabric {
      |     public Object compute(String id, ObjectNode context) {
      |		return context.toString();
      |    }
      |}
      | """.stripMargin

  val stencilComponents =
    s"""
      |{
      |	"name" : "PN${StringUtils.generateRandomStr(4)}" ,
      |	"components" :  "data"
      |}
    """.stripMargin

  val stencilComponentsUpdated =
    s"""
      |{
      |	"name" : "PN-Updated${StringUtils.generateRandomStr(4)}" ,
      |	"components" :  "data"
      |}
    """.stripMargin


  val updateEngine = StencilEngine.VELOCITY

  val updateEngineFabric =
    """{
      |"message" : "$message" , "title" : "$title" , "id" : "$id" , "triggerSound" : "$triggerSound" , "notificationType" : "$notificationType"
      |}""".stripMargin
  val escapedUpdateEngineFabric = StringEscapeUtils.escapeJava(updateEngineFabric)

  val message = StringUtils.generateRandomStr(10)
  val id = StringUtils.generateRandomStr(10)
  val triggerSound = false
  val notificationType = "Text"
  val title = StringUtils.generateRandomStr(10)
  val bucket = "GLOBAL"

  val payload =
    s"""{
       |"message" : "$message",
       |"id" : "$id",
       |"triggerSound" : $triggerSound,
       |"notificationType" : "$notificationType",
       |"title": "$title"
       |}""".stripMargin

  val escapedEngineFabric = StringEscapeUtils.escapeJava(engineFabric)
  var stencil: Stencil = null


  val input =
    s"""
       |{
       |	"name": "Android-PN-1",
       |	"components": [{
       |	    "component" : "data",
       |		"engine": "VELOCITY",
       |		"engineFabric": "{\\"message\\" : \\"$message\\" , \\"title\\" : \\"$title\\" , \\"id\\" : \\"$id\\" , \\"triggerSound\\" : \\"$triggerSound\\" , \\"notificationType\\" : \\"$notificationType\\" }"
       |	}],
       |	"bucket": "GLOBAL"
       |}
       |""".stripMargin

  val update =
    s"""
        |{
        |	"name": "Android-PN-Updated",
        |	"components": [{
        |	    "component" : "data",
        |		"engine": "VELOCITY",
        |		"engineFabric": "{\\"message\\" : \\"$message\\" , \\"title\\" : \\"$title\\" , \\"id\\" : \\"$id\\" , \\"triggerSound\\" : \\"$triggerSound\\" , \\"notificationType\\" : \\"$notificationType\\" }"
        |	}],
        |	"bucket": "GLOBAL"
        |}
     """.stripMargin

  var stencilId = ""
  var stencilComponentsId = ""

  val bucketName = StringUtils.generateRandomStr(6)


  "Stencil test" should "return Ok for save" in {
    Post("/v1/stencils", HttpEntity(MediaTypes.`application/json`, input)).addHeader(header) ~>
      stencilRoute ~>
      check {
        val responseString = Await.result(response.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)
        val responseData = responseString.getObj[ObjectNode].get("response").asInstanceOf[ObjectNode].put("type", "RESPONSE").getJson.getObj[Response]
        stencilId = StringUtils.getDetail(responseData.data, "id").get.toString
        status shouldEqual StatusCodes.Created
      }

  }

  "Stencil test" should "return Ok for get" in {
    Get(s"/v1/stencils/$stencilId").addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Stencil test" should "return Ok for preview" in {
    Post(s"/v1/stencils/$stencilId/preview", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Stencil test" should "return Ok for update" in {
    Put(s"/v1/stencils/$stencilId", HttpEntity(MediaTypes.`application/json`, update)).addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Stencil test" should "return Ok for version get" in {
    Get(s"/v1/stencils/$stencilId/1").addHeader(header) ~>
      stencilRoute ~>
      check {
        println("response = " + response)
        status shouldEqual StatusCodes.OK
      }

  }

  "Stencil test" should "return Ok for version preview" in {
    Post(s"/v1/stencils/$stencilId/1/preview", HttpEntity(MediaTypes.`application/json`, payload)).addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Stencil test" should "return Ok for bucket create" in {
    Post(s"/v1/stencils/bucket/$bucketName").addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.Created

      }
  }

  "Stencil test" should "return Ok for bucket get" in {
    Get(s"/v1/stencils/bucket/$bucketName").addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Stencil test" should "return Ok for stencil components create" in {
    Post("/v1/stencils/components/registry", HttpEntity(MediaTypes.`application/json`, stencilComponents)).addHeader(header) ~>
      stencilRoute ~>
      check {
        val responseString = Await.result(response.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)
        val responseData = responseString.getObj[ObjectNode].get("response").asInstanceOf[ObjectNode].put("type", "RESPONSE").getJson.getObj[Response]
        stencilComponentsId = StringUtils.getDetail(responseData.data, "StencilComponents").get.asInstanceOf[Map[String, AnyRef]].getJson.getObj[StencilsEnsemble].id
        status shouldEqual StatusCodes.Created
      }
  }

  "Stencil test" should "return Ok for stencil components get" in {
    Get(s"/v1/stencils/components/registry/$stencilComponentsId").addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Stencil test" should "return Ok for stencil components update" in {
    Put(s"/v1/stencils/components/registry/$stencilComponentsId", HttpEntity(MediaTypes.`application/json`, stencilComponentsUpdated)).addHeader(header) ~>
      stencilRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

  "Stencil test" should "return all ensemble" in {
    Get("/v1/stencils/components/registry").addHeader(header) ~>
      stencilRoute ~>
      check {
        val responseString = Await.result(response.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)
        status shouldEqual StatusCodes.OK
      }
  }
}
