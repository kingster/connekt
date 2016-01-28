package com.flipkart.connekt.receptors.routes.Stencils

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.flipkart.connekt.commons.entities.StencilEngine
import com.flipkart.connekt.commons.iomodels.GenericResponse
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.routes.BaseRouteTest
import org.apache.commons.lang.StringEscapeUtils

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * @author aman.shrivastava on 25/01/16.
 */
class StencilsRouteTest extends BaseRouteTest {

  val stencilRoute = new StencilsRoute().stencils
  val engine = StencilEngine.GROOVY
  val engineFabric = """
                           |package com.flipkart.connekt.commons.entities.fabric;
                           |
                           |import com.fasterxml.jackson.databind.node.JsonNodeFactory;
                           |import com.fasterxml.jackson.databind.node.ObjectNode;
                           |
                           |public class ConnektSampleAppGroovy extends PNGroovyFabric {
                           |public ObjectNode getData(String id, ObjectNode context) {
                           |
                           |return JsonNodeFactory.instance.objectNode()
                           |                .put("message", context.get("message").asText("_phantomastray_"))
                           |                .put("id", context.get("id").asText("pqwx2p2x321122228w2t1wxt"))
                           |                .put("triggerSound", true)
                           |                .put("notificationType", "Text")
                           |                .put("title", context.get("title").asText("Do not go gentle into that good night."));
                           |    }
                           |}
                           |""".stripMargin

  val message = StringUtils.generateRandomStr(10)
  val id = StringUtils.generateRandomStr(10)
  val triggerSound = false
  val notificationType = "Text"
  val title = StringUtils.generateRandomStr(10)

  val payload =
    s"""{
       |"message" : "$message",
       |"id" : "$id",
       |"triggerSound" : $triggerSound,
       |"notificationType" : "$notificationType",
       |"title": "$title"
       |}
    """.stripMargin

  val escapedEngineFabric = StringEscapeUtils.escapeJava(engineFabric)

  val input = s"""
       |{
       |   "engine" : "$engine",
       |   "engineFabric" : "$escapedEngineFabric"
       |}
    """.stripMargin

  var stencilId = ""

  "Stencil test" should "return Ok for save" in {
    Post("/v1/stencils", HttpEntity(MediaTypes.`application/json`, input)).addHeader(header) ~>
      stencilRoute ~>
        check {
          val responseString = Await.result(response.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)
          val responseData = responseString.getObj[GenericResponse].response
          stencilId = StringUtils.getDetail(responseData.data, "id").get.toString
          status shouldEqual StatusCodes.Created
        }

  }

  "Stencil test" should "return Ok for get" in {
    Get(s"/v1/stencils/$stencilId", HttpEntity(MediaTypes.`application/json`, input)).addHeader(header) ~>
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




}
