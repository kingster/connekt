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
package com.flipkart.connekt.barklice.task

import com.codahale.metrics.MetricRegistry
import flipkart.cp.convert.chronosQ.core.{SchedulerSink, SchedulerStore, TimeBucket, SchedulerCheckpointer}
import flipkart.cp.convert.ha.worker.task.WorkerTaskFactory


/**
 * Created by tushar.mandar on 3/13/15.
 */
class WorkerFactory(checkpointer: SchedulerCheckpointer, schedulerStore: SchedulerStore, timeBucket: TimeBucket, schedulerSink: SchedulerSink, metricRegistry: MetricRegistry,appName:String) extends WorkerTaskFactory {
  
  def getTask(taskName: String): WorkerTask = {
    new WorkerTaskImpl(checkpointer, schedulerStore, timeBucket, schedulerSink, taskName,appName).setRegistry(metricRegistry)
  }
}
