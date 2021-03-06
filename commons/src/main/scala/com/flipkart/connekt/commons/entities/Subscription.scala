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

  @Column(name = "sink")
  var sink : EventSink  = _

  @Column(name = "createdBy")
  var createdBy : String = _

  @Column(name = "createdTS")
  var createdTS: Date = new Date(System.currentTimeMillis())

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTS: Date = new Date(System.currentTimeMillis())

  @Column(name = "stencilId")
  var stencilId : String = _

  @Column(name = "shutdownThreshold")
  var shutdownThreshold : Int = _

  @Column(name = "active")
  var active : Boolean = false

  def this(sId: String, sName: String, endpoint: EventSink, createdBy: String,
           stencilId:String, shutdownThreshold: Int) = {
    this
    this.id = sId
    this.name = sName
    this.sink = endpoint
    this.createdBy = createdBy
    this.stencilId = stencilId
    this.shutdownThreshold = shutdownThreshold
  }

  override def toString =
    s"Subscription($id, $name, ${sink.getJson}, $createdBy, ${createdTS.toString}, ${lastUpdatedTS.toString}, $stencilId, $shutdownThreshold)"
}
