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
import org.apache.commons.lang.StringUtils

class StencilsEnsemble {

  @Column(name = "id")
  var id: String = _

  @Column(name = "name")
  var name: String = _

  @Column(name = "components")
  var components: String = StringUtils.EMPTY

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTs: Date = new Date(System.currentTimeMillis())

  @Column(name = "updatedBy")
  var updatedBy: String = StringUtils.EMPTY

  @Column(name = "creationTS")
  var creationTS: Date = new Date(System.currentTimeMillis())

  @Column(name = "createdBy")
  var createdBy: String = StringUtils.EMPTY

  override def toString = s"StencilComponents($id, $name, $components)"

  def validate() = {
    require(name.isDefined, "`name` must be defined.")
    require(components.isDefined, "`components` must be defined.")
  }

}
