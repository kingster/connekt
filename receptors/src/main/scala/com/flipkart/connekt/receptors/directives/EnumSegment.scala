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
package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.PathMatcher1
import com.flipkart.connekt.commons.entities.Channel.Channel
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.entities.UserType.UserType
import com.flipkart.connekt.commons.entities.{Channel, MobilePlatform, UserType}

abstract class EnumSegment[T <: Enumeration#Value](clz : Class[_])(implicit manifest : Manifest[T]) extends PathMatcher1[T] {

  def apply(path: Path) = path match {
    case Path.Segment(segment, tail) ⇒
      try{
        val method = Class.forName(clz.getEnumClassName).getMethod("withName", classOf[String])
        method.setAccessible(true)
        val value = method.invoke(null, segment).asInstanceOf[T]
        Matched(tail, Tuple1(value))
      } catch {
        case e:Exception =>
          Unmatched
      }
    case _ ⇒
      Unmatched
  }

  implicit class enumClassExtractor(val clz: Class[_]) {
    def getEnumClassName  = clz.getName.stripSuffix("$")
  }

}


object ChannelSegment extends EnumSegment[Channel](Channel.getClass)
object MPlatformSegment extends EnumSegment[MobilePlatform](MobilePlatform.getClass)
object UserTypeSegment extends EnumSegment[UserType](UserType.getClass)
