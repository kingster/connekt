package callback

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.{Subscription, SubscriptionAction}
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils._
import org.codehaus.jettison.json.JSONObject

import scala.concurrent.{ExecutionContext, Promise}

/**
  * Created by harshit.sinha on 13/06/16.
  */

class ClientTopologyManager()(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) extends SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.SUBSCRIPTION))

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
        val action = args.head.getJson
        val jObject: JSONObject = new JSONObject(action)
        val subscription = args.tail(0).getJson.getObj[Subscription]

        if (jObject.get("value").equals(SubscriptionAction.START.toString)) {
          if (!isTopologyActive(subscription.id)) startTopology(subscription)
        }
        else if (  jObject.get("value").equals(SubscriptionAction.STOP.toString)) {
          if (isTopologyActive(subscription.id)) getTrigger(subscription.id).success("User Signal shutdown")
        }
    }
  }

}

object ClientTopologyManager {
  var instance: ClientTopologyManager = null

  def apply()(implicit am: ActorMaterializer, sys: ActorSystem, ec: ExecutionContext) = {
    if (null == instance)
      this.synchronized {
        instance = new ClientTopologyManager()
      }
    instance
  }
}
