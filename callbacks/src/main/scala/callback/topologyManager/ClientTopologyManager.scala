package callback.topologyManager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import callback.stages.ClientTopology
import com.flipkart.connekt.commons.core.Wrappers._
import com.flipkart.connekt.commons.entities.Subscription
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils._

import scala.concurrent.ExecutionContext

/**
  * Created by harshit.sinha on 13/06/16.
  */
class ClientTopologyManager()(implicit am: ActorMaterializer,sys: ActorSystem, ec: ExecutionContext) extends  SyncDelegate {

  SyncManager.get().addObserver(this, List(SyncType.SUBSCRIPTION_REQUEST))
  private val activeTopology = scala.collection.mutable.Map[String, ClientTopology]()


  def findTopologyExistsActive(sId: String): Boolean = activeTopology.get(sId).isDefined

  def getTopologyActive(sId: String): ClientTopology = activeTopology(sId)

  /**
    * this will start a topology corresponding to the subscription sent
    * should not be called when a topology corresponding to that subscription.sId is already running
    *
    * @param subscription
    */
  def startTopology(subscription: Subscription): Unit = {
    val clientTopology = new ClientTopology(subscription, this)
    clientTopology.onStart()
    activeTopology += subscription.sId -> clientTopology
  }

  /**
    * this will stop a stream that is running
    * must not be called on subscription that don't have a activeTopology already running
    *
    * @param subscription
    */
  def stopTopology(subscription: Subscription) : Unit = {
    val clientTopology = getTopologyActive(subscription.sId)
    clientTopology.onStop()
    activeTopology -= subscription.sId
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Any = {
    _type match {
      case SyncType.SUBSCRIPTION_REQUEST => Try_ {

        val action = args.head.toString
        val subscription  = args.tail(0).getJson.getObj[Subscription]

        if(action == "start") { if(!findTopologyExistsActive(subscription.sId)) startTopology(subscription) }
        else if(action == "stop") { if (findTopologyExistsActive(subscription.sId)) stopTopology(subscription) }

      }
      case _ =>
    }
  }

  def onStop(): Unit =
  {
    //if needs to write the acitve subscription to the database
  }
  def onStart(): Unit ={
    //if need to restart the subscription that are in active state when the app went down
  }



}
