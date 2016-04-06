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
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.iomodels.CallbackEvent

import scala.util.Try

trait TCallbackDao extends Dao {
  def asyncSaveCallbackEvent(requestId: String, forContact: String, eventId: String, callbackEvent: CallbackEvent): Try[String]

  def saveCallbackEvent(requestId: String, forContact: String, eventId: String, callbackEvent: CallbackEvent): Try[String]

  def deleteCallbackEvents(requestId: String, forContact: String ): List[CallbackEvent]

  def fetchCallbackEvents(requestId: String, forContact: String, timestampRange: Option[(Long, Long)], maxRowsLimit: Option[Int] = None): List[CallbackEvent]
}
