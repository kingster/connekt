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
package com.flipkart.connekt.commons.utils

import io.netty.util.concurrent.FutureListener

import scala.concurrent.{Future, Promise}

object FutureUtils {

  implicit class NFutureAsScala[V](val f: io.netty.util.concurrent.Future[V]) {
    def asScala:Future[V] = {
      val result = Promise[V]()
      f.addListener(new FutureListener[V] {
        override def operationComplete(future: io.netty.util.concurrent.Future[V]): Unit = {
          if(future.isSuccess)
            result.success(future.getNow)
          else
            result.failure(future.cause())
        }
      })
      result.future
    }
  }

}
