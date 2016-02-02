package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.utils.StringUtils

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try
import com.flipkart.connekt.commons.utils.StringUtils._
/**
 * Created by kinshuk.bairagi on 02/02/16.
 */
class HttpDispatcher[V: ClassTag](uri: URL, method: HttpMethod, headers: scala.collection.immutable.Seq[HttpHeader], payloadCreator: (V) => RequestEntity) extends GraphStage[FlowShape[V, Try[HttpResponse]]] {

  val in = Inlet[V]("HttpDispatcher.In")
  val out = Outlet[Try[HttpResponse]]("HttpDispatcher.Out")

  override def shape: FlowShape[V, Try[HttpResponse]] = FlowShape.of(in, out)

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = ActorMaterializer()

  lazy val poolClientFlow = Http().cachedHostConnectionPool[String](uri.getHost, uri.getPort)


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {

        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPush:: Received Message: ${message.toString}")

        val request = new HttpRequest(method, uri.getPath,
          headers, payloadCreator(message)
        )

        val requestId = StringUtils.generateRandomStr(10)

        val responseFuture: Future[(Try[HttpResponse], String)] =
          Source.single(request -> requestId)
            .via(poolClientFlow)
            .runWith(Sink.head)

        responseFuture.map(response => push(out, response._1))
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