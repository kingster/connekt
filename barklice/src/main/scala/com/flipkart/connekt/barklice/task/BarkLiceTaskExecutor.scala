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

import java.util

import com.codahale.metrics.{Gauge, MetricRegistry, Timer}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.ConnektConfig
import flipkart.cp.convert.chronosQ.core.{SchedulerCheckpointer, SchedulerSink, SchedulerStore, TimeBucket}
import flipkart.cp.convert.chronosQ.exceptions.SchedulerException

import scala.util.Try


class BarkLiceTaskExecutor(checkPointer: SchedulerCheckpointer, schedulerStore: SchedulerStore, timeBucket: TimeBucket, schedulerSink: SchedulerSink, taskName: String, appName: String) extends Runnable {

  val partitionNumber = taskName.toInt

  private val batchSize = ConnektConfig.getOrElse(s"scheduler.worker.$appName.batchSize", "1000").toInt
  private val minSleepingTime = ConnektConfig.getOrElse(s"scheduler.worker.$appName.sleepTimeMilliSec", "500").toLong

  private var sinkPushingTime: Timer = _

  def setRegistry(metricRegistry: MetricRegistry): BarkLiceTaskExecutor = {
    ConnektLogger(LogFile.WORKERS).info(s"BarkLiceTaskExecutor starting for app: [$appName], partition: $partitionNumber")

    sinkPushingTime = metricRegistry.timer(s"sinkPublish.$appName.p-$partitionNumber")
    try {
      metricRegistry.register(MetricRegistry.name(classOf[BarkLiceTaskExecutor], s"nextExecWait.$appName.p-$partitionNumber"), new Gauge[Long] {
          override def getValue: Long = {
            Try(getCurrentEpoch - calculateNextIntervalForProcess(partitionNumber)).recover{
              case e: SchedulerException =>
                ConnektLogger(LogFile.WORKERS).error("BarkLiceTaskExecutor error", e)
                Long.MinValue
            }.get
          }
        })
    } catch {
      case e @(_: IllegalArgumentException | _: Exception) => ConnektLogger(LogFile.WORKERS).error(s"BarkLiceTaskExecutor failed setting metric registry for appName: $appName", e)
    }
    this
  }

  @throws(classOf[SchedulerException])
  private def calculateNextIntervalForProcess(partitionNum: Int): Long = {
    val timerKey: String = checkPointer.peek(partitionNum)
    val timerKeyConverted: Long = timerKey.toLong
    timeBucket.toBucket(timerKeyConverted)
  }
 
  private  def getCurrentEpoch = System.currentTimeMillis()

  override def run() = {
    while (!Thread.currentThread.isInterrupted) {
      try {
        var nextIntervalForProcess = calculateNextIntervalForProcess(partitionNumber)
        while (nextIntervalForProcess <= getCurrentEpoch) {
          var values: util.List[String] = null
          do {
            var time = System.currentTimeMillis()
            values = schedulerStore.getNextN(nextIntervalForProcess, partitionNumber, batchSize)
            ConnektLogger(LogFile.WORKERS).debug(s"BarkLiceScheduler get ${values.size} entries took: ${System.currentTimeMillis() - time} for appName: $appName partition: $partitionNumber")

            if (!values.isEmpty) {
              val context: Timer.Context = sinkPushingTime.time
              time = System.currentTimeMillis()
              schedulerSink.giveExpiredListForProcessing(values)
              ConnektLogger(LogFile.WORKERS).debug(s"BarkLiceScheduler push ${values.size} entries took: ${System.currentTimeMillis() - time} for appName: $appName partition: $partitionNumber")

              time = System.currentTimeMillis()
              schedulerStore.removeBulk(nextIntervalForProcess, partitionNumber, values)
              ConnektLogger(LogFile.WORKERS).debug(s"BarkLiceScheduler remove ${values.size} entries took: ${System.currentTimeMillis() - time} for appName: $appName partition: $partitionNumber")
              context.stop
            }
          } while (values.size != 0)

          checkPointer.set(String.valueOf(nextIntervalForProcess), partitionNumber)
          ConnektLogger(LogFile.WORKERS).info(s"BarkLiceTaskExecutor nextProcessTime: $nextIntervalForProcess in $appName partition $partitionNumber")
          nextIntervalForProcess = timeBucket.next(nextIntervalForProcess)
        }

        val timeLeftForNextInterval = nextIntervalForProcess - getCurrentEpoch
        if (timeLeftForNextInterval > 0){
          ConnektLogger(LogFile.WORKERS).debug(s"BarkLiceTaskExecutor sleep for $timeLeftForNextInterval")
          Thread.sleep(math.max(minSleepingTime, timeLeftForNextInterval))
        }
      }
      catch {
        case e: Exception =>
          ConnektLogger(LogFile.WORKERS).error("BarkLiceTaskExecutor failure.", e)
          Thread.sleep(5000)
      }
    }
  }
}
