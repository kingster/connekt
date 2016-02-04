package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.net.URL

import akka.http.javadsl.model.HttpEntityStrict
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.utils.StringUtils

import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

/**
 * Created by kinshuk.bairagi on 02/02/16.
 */

case class HttpResponseString(reponseCode:Int, responseBody:String) extends Serializable

class HttpDispatcher[V: ClassTag](uri: URL, method: HttpMethod, headers: scala.collection.immutable.Seq[HttpHeader], payloadCreator: (V) => RequestEntity) extends GraphStage[FlowShape[V, Try[HttpResponseString]]] {

  val in = Inlet[V]("HttpDispatcher.In")
  val out = Outlet[Try[HttpResponseString]]("HttpDispatcher.Out")

  override def shape: FlowShape[V, Try[HttpResponseString]] = FlowShape.of(in, out)

  implicit val system = BusyBeesBoot.system
  implicit val ec = BusyBeesBoot.system.dispatcher
  implicit val mat = BusyBeesBoot.mat

  lazy implicit val poolClientFlow = Http().cachedHostConnectionPoolTls[String](uri.getHost, uri.getPort)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = try {

        val message = grab(in)
        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPush:: Received Message: ${message.toString}")

        val request = new HttpRequest(method, uri.getPath,
          headers, payloadCreator(message)
        )

        val requestId = StringUtils.generateRandomStr(10)

        ConnektLogger(LogFile.PROCESSORS).info(s"HttpDispatcher:: onPush:: Request Payload : ${request.entity.asInstanceOf[HttpEntityStrict].data().decodeString("UTF-8")}")

        val responseFuture: Future[(Try[HttpResponse], String)] = Source.single(request -> requestId)
          .via(poolClientFlow).runWith(Sink.head)

        responseFuture.onComplete {
          case Success(tr) =>
            tr._1 match {
              case Success(response) =>
                val txtResponse = Await.result(response.entity.toStrict(10.seconds).map(_.data.decodeString("UTF-8")), 10.seconds)
                val ret = HttpResponseString(response.status.intValue(),txtResponse)
                ConnektLogger(LogFile.PROCESSORS).error(s"HttpDispatcher:: onPush :: HttpResponse "+ ret)
                push(out, Success(ret))
              case Failure(e) =>
                push(out, Failure(e))
            }
          case Failure(ex) =>
            push(out, Failure(ex))
        }

      } catch {
        case e: Throwable =>
          ConnektLogger(LogFile.PROCESSORS).error(s"HttpDispatcher:: onPush :: Error", e)
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