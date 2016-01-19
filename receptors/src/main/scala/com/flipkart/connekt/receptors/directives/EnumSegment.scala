package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.PathMatcher1


/**
 * @author aman.shrivastava on 18/01/16.
 */

abstract class EnumSegment[T <: Enumeration#Value](clz : String)(implicit manifest : Manifest[T]) extends PathMatcher1[T] {


  def apply(path: Path) = path match {
    case Path.Segment(segment, tail) ⇒
      try{

        val method = Class.forName(clz).getMethod("withName", classOf[String])
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
}

object ChannelSegment  extends EnumSegment[com.flipkart.connekt.commons.entities.Channel.Value]("com.flipkart.connekt.commons.entities.Channel")

//object ChannelSegment  extends EnumSegment[com.flipkart.connekt.commons.entities.Channel.Value](Channel.getClass.getName)


