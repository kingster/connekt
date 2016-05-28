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


class WorkerFactory(appName: String, checkPointer: SchedulerCheckpointer, schedulerStore: SchedulerStore, timeBucket: TimeBucket, schedulerSink: SchedulerSink, metricRegistry: MetricRegistry) extends WorkerTaskFactory {
  
  def getTask(taskName: String): Runnable = 
    new BarkLiceTaskExecutor(checkPointer, schedulerStore, timeBucket, schedulerSink, taskName,appName).setRegistry(metricRegistry)
}
