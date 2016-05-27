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
package com.flipkart.connekt.scheduler.di

import java.lang.annotation.Annotation
import java.util

import com.flipkart.connekt.commons.metrics.MetricRegistry
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.scheduler.task.WorkerFactory
import flipkart.cp.convert.chronosQ.core.impl.SecondGroupedTimeBucket
import flipkart.cp.convert.chronosQ.core.{SchedulerSink, TimeBucket, SchedulerCheckpointer, SchedulerStore}
import flipkart.cp.convert.ha.worker.di.WorkerModule
import flipkart.cp.convert.ha.worker.task.{WorkerTaskFactory, TaskList}

//app name as short as possible as this is part of row key
@WorkerModule(appName = "lo")
class ConnektDefaultPrioritySchedulerModule extends ConnektSchedulerModule {

  override protected def initializeClassMembers() {
    val aClass: Class[_] = classOf[ConnektDefaultPrioritySchedulerModule]
    val annotation: Annotation = aClass.getAnnotation(classOf[WorkerModule])
    val workerModuleAnnotation = annotation.asInstanceOf[WorkerModule]
    this.appName = workerModuleAnnotation.appName
    this.appVersion = DEFAULT_VERSION
    this.refreshInterval = 0
  }

  override protected def configureTaskList() {
    //Increasing partitions is not issue but for decreasing we need to move scheduled entry in higher partitions to new partition distribution
    NUM_PARTITIONS = ConnektConfig.getInt("scheduler.priority.low.partitions").getOrElse(96)
    bind(classOf[TaskList]).toInstance(new TaskList {

      private val list: util.ArrayList[String] = {
        val initialList = new util.ArrayList[String](NUM_PARTITIONS)
        (0 to NUM_PARTITIONS - 1).foreach(i => initialList.add(i.toString))
        initialList
      }

      def getTaskNames: util.ArrayList[String] = list
    })
  }


  override protected def configureWorkerTaskFactory() {
    bind(classOf[WorkerTaskFactory]).toInstance(getWorkerTaskFactory)
  }

  private def getWorkerTaskFactory: WorkerTaskFactory = {
    try {
      val hbaseSchedulerStore: SchedulerStore = configureStore
      val hbaseSchedulerCheckpointer: SchedulerCheckpointer = configureCheckPoint
      val minuteTimeBucket: TimeBucket = configureTimeBucket
      val kafkaSchedulerSink: SchedulerSink = configureSink
      new WorkerFactory(hbaseSchedulerCheckpointer, hbaseSchedulerStore, minuteTimeBucket, kafkaSchedulerSink, MetricRegistry.REGISTRY, appName)
    }
    catch {
      case ex: Exception =>
        throw new RuntimeException(ex)
    }
  }

  private def configureTimeBucket: TimeBucket = {
    new SecondGroupedTimeBucket(ConnektConfig.getInt("scheduler.priority.low.time.bucket").getOrElse(600)) //10min bucket
  }


}
