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
package com.flipkart.connekt.firefly

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Subscription
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.Config

import scala.concurrent.Promise
import scala.util.{Failure, Success}

class ClientTopologyManager(kafkaConsumerConnConf: Config, spoutTopic: String, eventRelayRetryLimit: Int)(implicit am: ActorMaterializer, sys: ActorSystem) extends SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.SUBSCRIPTION))

  private val triggers = scala.collection.mutable.Map[String, Promise[String]]()

  private def isTopologyActive(id: String): Boolean = triggers.get(id).isDefined

  private def getTrigger(id: String): Promise[String] = triggers(id)

  private def startTopology(subscription: Subscription): Unit = {
    val promise = new ClientTopology(spoutTopic, eventRelayRetryLimit, kafkaConsumerConnConf, subscription).start()
    triggers += subscription.id -> promise
    promise.future.onComplete( t => triggers -= subscription.id)(am.executionContext)
  }

  override def onUpdate(syncType: SyncType, args: List[AnyRef]): Any = {
    syncType match {
      case SyncType.SUBSCRIPTION =>
        val action = args.head.toString
        val subscription = args.last.toString.getObj[Subscription]
        action match {
          case "start" if !isTopologyActive(subscription.id) =>
            ConnektLogger(LogFile.SERVICE).info(s"Starting client topology ${subscription.id}")
            startTopology(subscription)
          case "stop" if isTopologyActive(subscription.id) =>
            ConnektLogger(LogFile.SERVICE).info(s"Stopping client topology ${subscription.id}")
            getTrigger(subscription.id).success("User initiated topology shutdown")
          case _ =>
            ConnektLogger(LogFile.SERVICE).warn(s"Unhandled client topology state $action")
        }
    }
  }

  def restoreState() ={
    SubscriptionService.getAll() match {
      case Success(subscriptions) => subscriptions.foreach((sub: Subscription) => if(sub.state) startTopology(sub))
      case Failure(e) => ConnektLogger(LogFile.SERVICE).error(e)
    }
  }

  def stopAllTopologies() = {
    ConnektLogger(LogFile.SERVICE).info("Shutting down `firefly`")
    triggers.keySet.foreach(s => {
      triggers(s).success(s"Stopping client topology $s on firefly shutdown.")
      ConnektLogger(LogFile.SERVICE).info("Stopping client topology $s on firefly shutdown")
    })
  }
}

object ClientTopologyManager {
  var instance: ClientTopologyManager = null

  def apply(kafkaConsumerConnConf: Config, spoutTopic: String, eventRelayRetryLimit: Int)(implicit am: ActorMaterializer, sys: ActorSystem) = {
    if (null == instance)
      this.synchronized {
        instance = new ClientTopologyManager(kafkaConsumerConnConf, spoutTopic, eventRelayRetryLimit)(am, sys)
        instance.restoreState()
      }
    instance
  }
}
