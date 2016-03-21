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

class Key {
  
  @Column(name = "key")
  var keyName: String = _

  @Column(name = "kind")
  var kind: String = _

  @Column(name = "value")
  var value: Array[Byte] = _

  @Column(name = "creationTS")
  var creationTS: Date = _

  @Column(name = "expireTS")
  var expireTS: Date = _

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTS: Date = _

  def this(key: String,
           kind: String,
           value: Array[Byte],
           creationTS: Date,
           lastUpdatedTS: Date,
           expireTS: Date
            ) {
    this()
    this.keyName = key
    this.kind = kind
    this.value = value
    this.creationTS = creationTS
    this.lastUpdatedTS = lastUpdatedTS
    this.expireTS = expireTS
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AppUser]

  override def equals(other: Any): Boolean = other match {
    case that: Key =>
      (that canEqual this) &&
        keyName == that.keyName &&
        value == that.value &&
        creationTS == that.creationTS &&
        lastUpdatedTS == that.lastUpdatedTS &&
        expireTS == that.expireTS
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(keyName, value, creationTS, lastUpdatedTS)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
