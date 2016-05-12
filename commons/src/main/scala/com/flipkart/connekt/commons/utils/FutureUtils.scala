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

import scala.concurrent.{ExecutionContext, Future, Promise}

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

  /**
   * [[ExecutionContext]] that will execute actions in calling thread (and by that making them blocking).
   */
  class CallingThreadExecutionContext extends ExecutionContext {

    override def execute(runnable: Runnable): Unit = runnable.run()

    override def reportFailure(t: Throwable): Unit = throw t
  }

  val callingThreadExecutionContext = new CallingThreadExecutionContext()

}
