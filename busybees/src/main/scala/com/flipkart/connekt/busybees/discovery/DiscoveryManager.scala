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
package com.flipkart.connekt.busybees.discovery

import com.typesafe.config.Config
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.client.ConnectStringParser

import collection.JavaConversions._

object DiscoveryManager {
  val instance = new DiscoveryManager()
}

sealed class DiscoveryManager {

  private var curatorClient: CuratorFramework = _
  private var pathCacheZookeeper: PathCacheZookeeper = _
  private var serviceHostsDiscovery : ServiceHostsDiscovery = _
  private final val serviceName =  "busybees"

  def init(config: Config): Unit = {
    val zookeeperConnekt = config.getString("curator.zk.connect")
    val baseSleepInMillis = config.getInt("curator.baseSleepInMilliSecs")
    val maxRetryCount = config.getInt("curator.maxRetryCount")
    val zookeeperSessionTimeoutInMillis = config.getInt("curator.zk.sessionTimeoutInMillis")
    val zookeeperConnectionTimeoutInMillis = config.getInt("curator.zk.connectionTimeoutInMillis")
    val retryPolicy = new ExponentialBackoffRetry(baseSleepInMillis, maxRetryCount)

    val (zkHosts, zkPath) = {
      val zkParser = new ConnectStringParser(zookeeperConnekt)
      zkParser.getServerAddresses.toList.map(add =>  s"${add.getHostName}:${add.getPort}").mkString(",")  -> zkParser.getChrootPath
    }

    curatorClient = CuratorFrameworkFactory.newClient(zkHosts, zookeeperSessionTimeoutInMillis, zookeeperConnectionTimeoutInMillis, retryPolicy)
    curatorClient.start()
    serviceHostsDiscovery = new ServiceHostsDiscovery(zkPath, curatorClient)
    pathCacheZookeeper = new PathCacheZookeeper(curatorClient, serviceHostsDiscovery, zkPath,serviceName)
  }

  def start(): Unit = {
    pathCacheZookeeper.add()
  }

  def shutdown(): Unit = {
    pathCacheZookeeper.removeAndClose()
    curatorClient.close()
  }

  def getInstances: List[String] = {
    serviceHostsDiscovery.listInstances(serviceName)
  }

}


