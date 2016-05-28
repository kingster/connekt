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

import java.util.List

import com.codahale.metrics.{Gauge, MetricRegistry, Timer}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.ConnektConfig
import flipkart.cp.convert.chronosQ.core.{SchedulerSink, TimeBucket, SchedulerStore, SchedulerCheckpointer}
import flipkart.cp.convert.chronosQ.exceptions.SchedulerException

import scala.util.Try


abstract class WorkerTask(taskName: String) extends Runnable {

  val partitionNumber = taskName.toInt
 
  def run() {
    process()
  }

  def process()
}

class WorkerTaskImpl(checkPointer: SchedulerCheckpointer, schedulerStore: SchedulerStore, timeBucket: TimeBucket, schedulerSink: SchedulerSink, taskName: String, appName: String) extends WorkerTask(taskName) {

  private val BATCH_SIZE = ConnektConfig.getOrElse(s"scheduler.worker.$appName.batchSize", "1000").toInt
  private val MIN_SLEEP_TIME = ConnektConfig.getOrElse(s"scheduler.worker.$appName.sleepTimeMilliSec", "500").toLong

  private var metricRegistry: MetricRegistry = _
  private var sinkPushingTime: Timer = _

  def setRegistry(metricRegistry: MetricRegistry): WorkerTaskImpl = {
    ConnektLogger(LogFile.WORKERS).info(s"WorkerTaskImpl Starting for APP [$appName], Partition : $partitionNumber ")
    this.metricRegistry = metricRegistry
    sinkPushingTime = metricRegistry.timer(s"sinkPushing.$appName.Partition-$partitionNumber")
    try {
      metricRegistry.register(MetricRegistry.name(classOf[WorkerTaskImpl], s"ElapsedTimeWorkerToProcess.$appName.Partition-$partitionNumber"), new Gauge[Long] {
          override def getValue: Long = {
            Try(getCurrentEpoch - calculateNextIntervalForProcess(partitionNumber)).recover{
              case e: SchedulerException =>
                ConnektLogger(LogFile.WORKERS).error("Scheduler WorkerTaskImpl Exception happened ", e)
                Long.MinValue
            }.get
          }
        })
    } catch {
      case e: IllegalArgumentException => ConnektLogger(LogFile.WORKERS).error(s"ERROR while setting registry for $appName", e)
      case e: Exception => ConnektLogger(LogFile.WORKERS).error(s"ERROR while setting registry for $appName"); throw e
    }
    this
  }

  def process() {
    while (!Thread.currentThread.isInterrupted) {
      try {
        var nextIntervalForProcess = calculateNextIntervalForProcess(partitionNumber)
        while (nextIntervalForProcess <= getCurrentEpoch) {
          var values: List[String] = null
          do {
            var time = System.currentTimeMillis()
            values = schedulerStore.getNextN(nextIntervalForProcess, partitionNumber, BATCH_SIZE)
            ConnektLogger(LogFile.WORKERS).debug(s"SCHEDULER GET T=${System.currentTimeMillis() - time} For values:${values.size} $appName partition $partitionNumber")
            if (!values.isEmpty) {
              val context: Timer.Context = sinkPushingTime.time
              time = System.currentTimeMillis()
              schedulerSink.giveExpiredListForProcessing(values)
              ConnektLogger(LogFile.WORKERS).debug(s"SCHEDULER PUSH T=${System.currentTimeMillis() - time} For values:${values.size} $appName partition $partitionNumber")
              time = System.currentTimeMillis()
              schedulerStore.removeBulk(nextIntervalForProcess, partitionNumber, values)
              ConnektLogger(LogFile.WORKERS).debug(s"SCHEDULER  DELETE T=${System.currentTimeMillis() - time} For values:${values.size} $appName partition $partitionNumber")
              context.stop
            }
          } while (values.size != 0)
          checkPointer.set(String.valueOf(nextIntervalForProcess), partitionNumber)
          ConnektLogger(LogFile.WORKERS).info(s"Processed for  $nextIntervalForProcess in $appName partition $partitionNumber")
          nextIntervalForProcess = timeBucket.next(nextIntervalForProcess)
        }
        val timeLeftForNextInterval = nextIntervalForProcess - getCurrentEpoch
        if (timeLeftForNextInterval > 0){
          ConnektLogger(LogFile.WORKERS).debug(s"SCHEDULER Sleep for $timeLeftForNextInterval")
          Thread.sleep(math.max(MIN_SLEEP_TIME, timeLeftForNextInterval))
        }
      }
      catch {
        case ex: Exception =>
          ConnektLogger(LogFile.WORKERS).error("SCHEDULER Exception happened", ex)
          Thread.sleep(5000)
        //TODO : add alerting
      }
    }
  }

  @throws(classOf[SchedulerException])
  private def calculateNextIntervalForProcess(partitionNum: Int): Long = {
    val timerKey: String = checkPointer.peek(partitionNum)
    val timerKeyConverted: Long = timerKey.toLong
    timeBucket.toBucket(timerKeyConverted) //returns interval in sec as we are using SecondGroupedBucket
  }
 
  private  def getCurrentEpoch = System.currentTimeMillis()

}
