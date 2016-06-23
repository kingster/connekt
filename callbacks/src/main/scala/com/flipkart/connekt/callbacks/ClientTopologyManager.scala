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
package com.flipkart.connekt.callbacks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.{Subscription, GenericAction}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.Config

import scala.concurrent.Promise

class ClientTopologyManager(kafkaConsumerConnConf: Config)(implicit am: ActorMaterializer, sys: ActorSystem) extends SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.SUBSCRIPTION))

  private val topic = ConnektConfig.getString("callbacks.kafka.source.topic").get
  private val retryLimit = ConnektConfig.getInt("callbacks.retry.limit").get

  private val triggers = scala.collection.mutable.Map[String, Promise[String]]()

  private def isTopologyActive(id: String): Boolean = triggers.get(id).isDefined

  private def getTrigger(id: String): Promise[String] = triggers(id)

  private def startTopology(subscription: Subscription): Unit = {
    val promise = new ClientTopology(topic, retryLimit, kafkaConsumerConnConf, subscription).start()
    triggers += subscription.id -> promise
    promise.future.onComplete(t => triggers -= subscription.id )(am.executionContext)

  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Any = {
    _type match {
      case SyncType.SUBSCRIPTION =>
        val action = GenericAction.withName(args.head.toString)
        val subscription = args.last.toString.getObj[Subscription]
        action match {
          case GenericAction.START if !isTopologyActive(subscription.id) =>
            startTopology(subscription)
          case GenericAction.STOP if isTopologyActive(subscription.id) =>
            getTrigger(subscription.id).success("User Signal shutdown")
          case _ =>
            ConnektLogger(LogFile.SERVICE).warn(s"Unhandled State $action")
        }
    }
  }

}

object ClientTopologyManager {
  var instance: ClientTopologyManager = null

  def apply(kafkaConsumerConnConf: Config)(implicit am: ActorMaterializer, sys: ActorSystem) = {
    if (null == instance)
      this.synchronized {
        instance = new ClientTopologyManager(kafkaConsumerConnConf)(am, sys)
      }
    instance
  }
}
