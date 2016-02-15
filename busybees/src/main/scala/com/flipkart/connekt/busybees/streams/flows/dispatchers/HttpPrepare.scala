package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URL

import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils

import scala.reflect.ClassTag

/**
 * Created by kinshuk.bairagi on 02/02/16.
 */
class HttpPrepare[V: ClassTag](uri: URL, method: HttpMethod, headers: scala.collection.immutable.Seq[HttpHeader], payloadCreator: (V) => RequestEntity)
  extends GraphStage[FlowShape[V, (HttpRequest, String)]] {

  val in = Inlet[V]("HttpPrepare.In")
  val out = Outlet[(HttpRequest, String)]("HttpPrepare.Out")

  override def shape: FlowShape[V, (HttpRequest, String)] = FlowShape.of(in, out)

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = BusyBeesBoot.mat

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = try {

        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPush:: Received Message: ${message.toString}")

        val request = new HttpRequest(method, uri.getPath, headers, payloadCreator(message))
        val requestId = StringUtils.generateRandomStr(10)

        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPush:: Request Payload : ${request.entity.asInstanceOf[Strict].data.decodeString("UTF-8")}")
        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPush:: Relayed Try[HttpResponse] to next stage for: $requestId")

        push(out, (request, requestId))
      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"HttpDispatcher:: onPush :: Error", e)
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPull")
        pull(in)
      }
    })

  }

}