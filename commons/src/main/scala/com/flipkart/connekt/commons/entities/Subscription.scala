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
package com.flipkart.connekt.commons.entities

import java.util.Date
import javax.persistence.Column

import com.flipkart.connekt.commons.utils.StringUtils._

class Subscription {

  @Column(name = "id")
  var id : String = _

  @Column(name = "name")
  var name: String = _

  @Column(name = "eventSink")
  var eventSink : EventSink  = _

  @Column(name = "createdBy")
  var createdBy : String = _

  @Column(name = "createdTS")
  var createdTS: Date = new Date(System.currentTimeMillis())

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTS: Date = new Date(System.currentTimeMillis())

  @Column(name = "eventFilter")
  var eventFilter : String = _

  @Column(name = "shutdownThreshold")
  var shutdownThreshold : Int = _

  def this(sId: String, sName: String, endpoint: EventSink,
           createdBy: String, createdTS: Date, lastUpdatedTS: Date, groovyString: String, shutdownThreshold: Int) = {
    this
    this.id = sId
    this.name = sName
    this.eventSink = endpoint
    this.createdBy = createdBy
    this.createdTS = createdTS
    this.lastUpdatedTS = lastUpdatedTS
    this.eventFilter = groovyString
    this.shutdownThreshold = shutdownThreshold
  }

  override def toString =
    s"Subscription($id, $name, ${eventSink.getJson}, $createdBy, ${createdTS.toString}, ${lastUpdatedTS.toString}, $eventFilter, ${shutdownThreshold.toString})"
}
