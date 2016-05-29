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
package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.iomodels.ConnektRequest
import com.flipkart.connekt.commons.metrics.MetricRegistry
import com.flipkart.connekt.commons.services.SchedulerService.ScheduledRequest
import com.flipkart.connekt.commons.utils.StringUtils._
import flipkart.cp.convert.chronosQ.client.SchedulerClient
import flipkart.cp.convert.chronosQ.core.SchedulerEntry
import flipkart.cp.convert.chronosQ.core.impl.{MurmurHashPartioner, SecondGroupedTimeBucket}
import flipkart.cp.convert.chronosQ.impl.hbase.HbaseSchedulerStore
import org.apache.hadoop.hbase.client.Connection

class SchedulerService( hConnection:Connection) extends TService {

  lazy val schedulerStore = ConnektConfig.getString("tables.hbase.scheduler.store").get
  lazy val schedulerStore_CF = ConnektConfig.getOrElse("scheduler.hbase.store.columnFamily", "d")

  val client = new SchedulerClient.Builder[ScheduledRequest]()
    .withStore(new HbaseSchedulerStore(hConnection,schedulerStore,schedulerStore_CF,"lo"))  //app name as short as possible as this is part of row key
    .withTimeBucket(new SecondGroupedTimeBucket(ConnektConfig.getInt("scheduler.priority.lo.time.bucket").getOrElse(600)))      // 30 min bucket
    .withPartitioner(new MurmurHashPartioner(ConnektConfig.getInt("scheduler.priority.lo.partitions").getOrElse(96)))  //Increasing partitions is not issue but for decreasing we need to move scheduled entry in higher partitions to new partition distribution
    .withMetricRegistry(MetricRegistry.REGISTRY)
    .buildOrGet
}

object  SchedulerService {

  private[commons] val delimiter = "$"

  case class ScheduledRequest(request: ConnektRequest, queueName: String) extends SchedulerEntry {
    override def getStringValue: String = {
      queueName + delimiter + request.getJson
    }
  }
}
