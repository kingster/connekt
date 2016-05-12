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
package com.flipkart.connekt.commons.cache

import scala.concurrent.duration.Duration
import scala.reflect.runtime.universe._

trait CacheManager {

}

abstract class Caches {

  def put[T](key: String, value: T)(implicit cTag: reflect.ClassTag[T]): Boolean

  def put[T](kv: List[(String, T)])(implicit cTag: reflect.ClassTag[T]): Boolean

  def get[T](key: String)(implicit cTag: reflect.ClassTag[T]): Option[T]

  /**
   *  TODO: Figure out something for not passing typeTag explicitly and not also effecting performance.
   *  This method is 20x slower than the normal method. Use this only when you have nested structs which are erasured.
   *
   * @param key
   * @param typeTag
   * @tparam T
   * @return
   */
  def get[T](key: String, typeTag: TypeTag[T])(implicit tTag: TypeTag[T]): Option[T]

  def get[T](keys: List[String])(implicit cTag: reflect.ClassTag[T]): Map[String,T]

  def get[T](keys: List[String], typeTag: TypeTag[T])(implicit tTag: TypeTag[T]): Map[String,T]

  def remove(key:String):Unit

  def exists(key: String): Boolean

  def flush(): Unit

}

case class CacheProperty(size: Long, ttl: Duration)
