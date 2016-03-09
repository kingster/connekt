package com.flipkart.connekt.commons.transmission

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future
import scala.util.Try

/**
 *
 *
 * @author durga.s
 * @version 11/28/15
 */
object HostConnectionHelper {

  implicit val system = ActorSystem("host-conn-helper")
  implicit val materializer = ActorMaterializer()

  def getPoolClientFlow[T](host: String, port: Int = 80) = {
    Http().cachedHostConnectionPoolHttps[T](host, port)
  }

  def terminate =
    Http().shutdownAllConnectionPools()

  def request[T](request: HttpRequest, requestIdentity: T)
                (implicit clientPoolFlow: Flow[(HttpRequest, T), (Try[HttpResponse], T), Http.HostConnectionPool]): Future[(Try[HttpResponse], T)] = {
    Source.single(request -> requestIdentity)
      .via(clientPoolFlow)
      .runWith(Sink.head)
  }
}
