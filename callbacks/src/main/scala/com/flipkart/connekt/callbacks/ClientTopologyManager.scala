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
import com.flipkart.connekt.commons.entities.{Subscription, SubscriptionAction}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Promise}

class ClientTopologyManager()(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext, kafkaConsumerConnConf: Config) extends SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.SUBSCRIPTION))

  implicit val topic = ConnektConfig.getString("callbacks.kafka.source.topic").get

  private val triggers = scala.collection.mutable.Map[String, Promise[String]]()

  private def isTopologyActive(id: String): Boolean = triggers.get(id).isDefined

  private def getTrigger(id: String): Promise[String] = triggers(id)

  def startTopology(subscription: Subscription): Unit = {
    val promise = new ClientTopology(subscription).start()
    triggers += subscription.id -> promise
    promise.future onComplete { t =>
      triggers -= subscription.id
    }
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Any = {
    _type match {
      case SyncType.SUBSCRIPTION =>
        val action = args.head
        val subscription = args.tail.head.getJson.getObj[Subscription]
        if (action.equals(SubscriptionAction.START.toString) && !isTopologyActive(subscription.id)) {
          startTopology(subscription)
        }
        else if (action.equals(SubscriptionAction.STOP.toString) && isTopologyActive(subscription.id)) {
          getTrigger(subscription.id).success("User Signal shutdown")
        }
    }
  }

}

object ClientTopologyManager {
  var instance: ClientTopologyManager = null

  def apply()(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext, kafkaConsumerConnConf: Config) = {
    if (null == instance)
      this.synchronized {
        instance = new ClientTopologyManager()
      }
    instance
  }
}
