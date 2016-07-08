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

import com.couchbase.client.java.Bucket
import com.couchbase.client.java.document.StringDocument
import com.couchbase.client.java.query.Query
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import rx.lang.scala.Observable

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt


class StatsReportingDao(bucket: Bucket) extends Dao {

  val ttl = 15.days.toSeconds.toInt

  def put(kv: List[(String, Long)]) =
    Observable.from(kv).flatMap(kv => {
      rx.lang.scala.JavaConversions.toScalaObservable(bucket.async().upsert(StringDocument.create(kv._1, ttl, kv._2.toString)))
    }).last.toBlocking.single


  def get(keys: List[String]): Predef.Map[String, Long] = {
    Observable.from(keys).flatMap(key => {
      rx.lang.scala.JavaConversions.toScalaObservable(bucket.async().get(StringDocument.create(key))).filter(_ != null).map(d => key -> d.content().toLong)
    }).toList.toBlocking.single.toMap
  }

  def counter(kvList: List[(String, Long)]) = {
    Observable.from(kvList).flatMap(kv => {
      rx.lang.scala.JavaConversions.toScalaObservable(bucket.async().counter(kv._1, kv._2, kv._2, ttl))
    }).last.toBlocking.single
  }

  def prefix(prefixString: String): List[String] = {
    try {

      val queryResult = bucket.query(
        Query.simple(s"SELECT META(${bucket.name()}).id FROM ${bucket.name()} WHERE META(${bucket.name()}).id LIKE '$prefixString%'")
      )

      queryResult.iterator().asScala.map(queryResult => {
        queryResult.value().get("id").toString
      }).toList

    } catch {
      case e: Exception =>
        ConnektLogger(LogFile.SERVICE).error("StatsReportingDao prefix search failure", e)
        throw e
    }
  }
}

object StatsReportingDao {
  def apply(bucket: Bucket) = new StatsReportingDao(bucket)
}
