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
package com.flipkart.connekt.commons.sync

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.utils.StringUtils._
import org.apache.curator.framework.recipes.cache._
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryForever
import org.apache.curator.utils.ZKPaths
import org.apache.zookeeper.{CreateMode, ZooDefs}

import scala.collection.mutable.ListBuffer
import scala.util.Try

class SyncManager(zkQuorum: String) {

  /** Zookeeper client */
  private val sessionTimeout = 100000
  private val connectionTimeout = 15000

  private val BUCKET_NODE_PATH: String = s"/connekt-sync-${ConnektConfig.getString("sync.cluster.name").getOrElse("local")}"

  /** CuratorFramework Client */
  private lazy val client: CuratorFramework = CuratorFrameworkFactory.newClient(zkQuorum, sessionTimeout, connectionTimeout, new RetryForever(1000))

  /** PathChildrenCache Watcher */
  private lazy val cache: TreeCache = new TreeCache(client, BUCKET_NODE_PATH)

  def initialize() {
    if (zkQuorum == null || zkQuorum.isEmpty) {
      throw new Exception("Creation of ZKClient Failed")
    }

    client.start()
    cache.start()
    addListeners()
    if (client.checkExists().forPath(BUCKET_NODE_PATH) == null) {
      client.create().forPath(BUCKET_NODE_PATH)
    }

  }

  private def addListeners() {
    cache.getListenable.addListener(new TreeCacheListener {
      override def childEvent(curatorFramework: CuratorFramework, event: TreeCacheEvent): Unit = {
        event.getType match {
          case TreeCacheEvent.Type.NODE_ADDED | TreeCacheEvent.Type.NODE_UPDATED =>
            val nodePath = ZKPaths.getNodeFromPath(event.getData.getPath)
            ConnektLogger(LogFile.SERVICE).info(s"Node Mod: [${event.getType}] " + nodePath)

            val payloadS = Option(event.getData.getData).map(_.getString)
            payloadS match {
              case Some(payload) =>
                BUCKET_NODE_PATH.tail.equals(nodePath) match {
                  case false =>
                    ConnektLogger(LogFile.SERVICE).info("Sync Message Received :" + payload)
                    val payloadObject = payload.getObj[SyncMessage]
                    postNotification(payloadObject.topic, payloadObject.message)
                  case true =>
                    ConnektLogger(LogFile.SERVICE).info("Cluster Join :" + payload)
                }
              case None =>
                ConnektLogger(LogFile.SERVICE).info("Sync Message NULL Data")
            }

          case TreeCacheEvent.Type.NODE_REMOVED =>
            ConnektLogger(LogFile.SERVICE).info("Node Del: " + ZKPaths.getNodeFromPath(event.getData.getPath));

          case _ =>
        }
      }
    })
  }

  private def getZK = {
    client.getZookeeperClient.getZooKeeper
  }


  def publish(message: SyncMessage): Try[Unit] = Try {
    ConnektLogger(LogFile.SERVICE).info("Sync Message Publishing : " + message.getJson)
    val nodePath = BUCKET_NODE_PATH + "/" + message.topic
    val stat = getZK.exists(nodePath, false)
    if (stat == null) {
      getZK.create(nodePath, message.getJson.getBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL)
    }
    else {
      getZK.setData(nodePath, message.getJson.getBytes, stat.getVersion)
    }
    //TODO: publish to self?
  }


  private val observersSync = new Object()
  private val observers = scala.collection.mutable.Map[SyncType, ListBuffer[AnyRef]]()
  private val removeAfterBroadcast = scala.collection.mutable.Map[SyncType, AnyRef]()
  private val addAfterBroadcast = scala.collection.mutable.Map[SyncType, AnyRef]()

  private var broadcasting: Int = 0

  private def postNotification(id: SyncType, args: List[AnyRef]) {
    broadcasting += 1
    if (observers.get(id).isDefined) {
      for (obj <- observers(id)) {
        obj.asInstanceOf[SyncDelegate].onUpdate(id, args)
      }
    }
    observersSync.synchronized {
      broadcasting -= 1
      if (broadcasting == 0) {
        if (removeAfterBroadcast.nonEmpty) {
          removeAfterBroadcast.foreach(entry => {
            removeObserver(entry._2, List(entry._1))
          })
          removeAfterBroadcast.clear()
        }
        if (addAfterBroadcast.nonEmpty) {
          addAfterBroadcast.foreach(entry => {
            addObserver(entry._2, List(entry._1))
          })
          addAfterBroadcast.clear()
        }
      }
    }
  }


  def addObserver(observer: AnyRef, ids: List[SyncType]): Unit = {
    ids.foreach(id => observersSync.synchronized {
      if (broadcasting != 0) {
        addAfterBroadcast.put(id, observer)
      } else {
        if (observers.get(id).isEmpty) {
          observers += id -> ListBuffer(observer)
        } else if (!observers(id).contains(observer)) {
          observers(id) += observer
        }
      }
    })
  }

  def removeObserver(observer: AnyRef, ids: List[SyncType]) {
    ids.foreach(id => observersSync.synchronized {
      if (broadcasting != 0) {
        removeAfterBroadcast.put(id, observer)
      } else if (observers.get(id).isDefined) {
        observers(id) -= observer
        if (observers(id).isEmpty)
          observers.remove(id)
      }
    })
  }

}

object SyncManager {

  var instance: SyncManager = null

  def create(zkQuorum: String) = {
    this.synchronized {
      if (null == instance) {
        instance = new SyncManager(zkQuorum)
        instance.initialize()
      }
    }
    instance
  }

  def get(): SyncManager = instance

}
