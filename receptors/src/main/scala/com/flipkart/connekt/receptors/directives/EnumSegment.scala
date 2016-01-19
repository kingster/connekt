package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.PathMatcher1
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.entities.Channel.Channel

/**
 * @author aman.shrivastava on 18/01/16.
 */

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


object ChannelSegment  extends EnumSegment[Channel](Channel.getClass)


