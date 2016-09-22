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
package com.flipkart.connekt.commons.core

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.util.{Failure, Success, Try}

object Wrappers {

  def Try_#[T](fileName: String = LogFile.SERVICE, message: String = "ERROR")(f: => T) : Try[T] = {
    try {
      Success(f)
    }
    catch {
      case e: Throwable =>
        ConnektLogger(fileName).error(s"$message, e: ${e.getMessage}",e)
        Failure(e)
    }
  }

  def Try_ [T](f: => T) : Try[T] = {
    Try_#()(f)
  }
}
