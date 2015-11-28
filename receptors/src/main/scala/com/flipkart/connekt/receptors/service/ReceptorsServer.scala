package com.flipkart.connekt.receptors.service

import _root_.akka.actor.ActorSystem
import _root_.akka.http.scaladsl.Http
import _root_.akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.receptors.routes.pn.{Unicast, Registration}

/**
 *
 *
 * @author durga.s
 * @version 11/20/15
 */
class ReceptorsServer {
  implicit val system = ActorSystem("connekt-receptors-as")
  implicit val materializer = ActorMaterializer()

  implicit val ec = system.dispatcher

  private val bindHost = ConnektConfig.getString("receptors.bindHost").getOrElse("0.0.0.0")
  private val bindPort = ConnektConfig.getInt("receptors.bindPort").getOrElse(25000)

  val receptorReqHandler = new Registration().register

  val unicastHandler = new Unicast().unicast

  lazy val init =
    Http().bindAndHandle(unicastHandler, bindHost, bindPort)

  def stop() = {

    init.flatMap(_.unbind())
      .onComplete(_ => {
      println("receptor server unbinding complete")
      system.terminate()
    })
  }

}
