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
package com.flipkart.connekt.barklice.di

import java.lang.annotation.Annotation
import java.util

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}
import com.flipkart.connekt.commons.metrics.MetricRegistry
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.barklice.task.WorkerFactory
import flipkart.cp.convert.chronosQ.core.impl.SecondGroupedTimeBucket
import flipkart.cp.convert.chronosQ.core.{SchedulerSink, TimeBucket, SchedulerCheckpointer, SchedulerStore}
import flipkart.cp.convert.ha.worker.di.WorkerModule
import flipkart.cp.convert.ha.worker.task.{WorkerTaskFactory, TaskList}

//app name as short as possible as this is part of row key
@WorkerModule(appName = "lo")
class ConnektDefaultPrioritySchedulerModule(hostname:String) extends ConnektSchedulerModule {

  override protected def initializeClassMembers() {
    val aClass: Class[_] = classOf[ConnektDefaultPrioritySchedulerModule]
    val annotation: Annotation = aClass.getAnnotation(classOf[WorkerModule])
    val workerModuleAnnotation = annotation.asInstanceOf[WorkerModule]
    this.appName = workerModuleAnnotation.appName
    this.refreshInterval = 0
  }

  override protected def configureTaskList() {
    //Increasing partitions is not issue but for decreasing we need to move scheduled entry in higher partitions to new partition distribution
    numPartitions = ConnektConfig.getInt("scheduler.priority.low.partitions").getOrElse(96)
    bind(classOf[TaskList]).toInstance(new TaskList {

      private val list: util.ArrayList[String] = {
        val initialList = new util.ArrayList[String](numPartitions)
        (0 to numPartitions - 1).foreach(i => initialList.add(i.toString))
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
      val hbaseSchedulerCheckPointer: SchedulerCheckpointer = configureCheckPoint
      val minuteTimeBucket: TimeBucket = configureTimeBucket
      val kafkaSchedulerSink: SchedulerSink = configureSink
      new WorkerFactory(appName, hbaseSchedulerCheckPointer, hbaseSchedulerStore, minuteTimeBucket, kafkaSchedulerSink, MetricRegistry.REGISTRY)
    }
    catch {
      case ex: Exception =>
        ConnektLogger(LogFile.WORKERS).error("getWorkerTaskFactory Exception", ex)
        throw new RuntimeException(ex)
    }
  }

  private def configureTimeBucket: TimeBucket = {
    new SecondGroupedTimeBucket(ConnektConfig.getInt("scheduler.priority.low.time.bucket").getOrElse(600)) //10min bucket
  }


}
