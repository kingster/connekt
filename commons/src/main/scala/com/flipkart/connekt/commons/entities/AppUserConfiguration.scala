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

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.flipkart.connekt.commons.entities.Channel._

class AppUserConfiguration {

  @Column(name = "userId")
  var userId: String = _

  @EnumTypeHint(value = "com.flipkart.connekt.commons.entities.Channel")
  @JsonSerialize(using = classOf[ChannelToStringSerializer])
  @JsonDeserialize(using = classOf[ChannelToStringDeserializer])
  @Column(name = "channel")
  var channel: Channel = _

  @Column(name = "platforms")
  var platforms: String = _

  @Column(name = "queueName")
  var queueName: String = _

  @Column(name = "maxRate")
  var maxRate: Int = _

  @Column(name = "errorThresholdRate")
  var errorThresholdRate: Int = -1

  @Column(name = "lastUpdatedTS")
  var lastUpdatedTS: Date = new Date(System.currentTimeMillis())

  def this(userId: String,
           channel: Channel,
           queueName: String,
           platforms: String,
           maxRate: Int) {
    this()
    this.userId = userId
    this.channel = channel
    this.queueName = queueName
    this.maxRate = maxRate
    this.platforms = platforms
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AppUserConfiguration]

  override def equals(other: Any): Boolean = other match {
    case that: AppUserConfiguration =>
      (that canEqual this) &&
        userId == that.userId &&
        channel == that.channel &&
        queueName == that.queueName &&
        platforms == this.platforms &&
        maxRate == that.maxRate &&
        errorThresholdRate == that.errorThresholdRate
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(userId, channel, platforms, queueName, maxRate, errorThresholdRate)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  def validate() = {
    require(null != userId && !userId.isEmpty, "user configuration must have `userId` specified")
    require(null != channel, "user configuration `channel` cannot be un-defined")
    require(maxRate > 0, "user configuration `maxRate` > 0")
  }
}
