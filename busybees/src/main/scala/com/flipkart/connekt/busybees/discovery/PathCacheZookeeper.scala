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

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.sync.{SyncManager, SyncType}
import com.flipkart.connekt.commons.utils.NetworkUtils
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.cache.{PathChildrenCache, PathChildrenCacheEvent, PathChildrenCacheListener}
import org.apache.curator.utils.CloseableUtils

class PathCacheZookeeper(curatorClient: CuratorFramework, serviceHostsDiscovery: ServiceHostsDiscovery, servicesRootPath: String, serviceName: String) {

  val cache: PathChildrenCache = new PathChildrenCache(curatorClient, servicesRootPath + "/" + serviceName, true)
  cache.start()

  def add() {
    ConnektLogger(LogFile.CLIENTS).info(s"Added $serviceName in path $servicesRootPath")
    serviceHostsDiscovery.addInstance(serviceName, NetworkUtils.getIPAddress)
    addListener()
  }

  def removeAndClose() {
    ConnektLogger(LogFile.CLIENTS).info(s"Removing $serviceName from path $servicesRootPath")
    serviceHostsDiscovery.deleteInstance(serviceName, NetworkUtils.getIPAddress)
    CloseableUtils.closeQuietly(cache)
    serviceHostsDiscovery.close()
    CloseableUtils.closeQuietly(curatorClient)
  }

  private def addListener() {
    ConnektLogger(LogFile.CLIENTS).info(s"Added listener to $serviceName in path $servicesRootPath")
    val listener = new PathChildrenCacheListener() {
      override def childEvent(curatorClient: CuratorFramework, event: PathChildrenCacheEvent) {
        if(curatorClient.getState == CuratorFrameworkState.STARTED) {
          val currentInstances = serviceHostsDiscovery.listInstances(serviceName)
          ConnektLogger(LogFile.CLIENTS).info(s"Total number of instance in path ${servicesRootPath + "/" + serviceName} = ${currentInstances.size}")
          ConnektLogger(LogFile.CLIENTS).info(s"Name of instances in path ${servicesRootPath + "/" + serviceName} = ${currentInstances.toString()}")
          SyncManager.instance.postNotification(SyncType.DISCOVERY_CHANGE, currentInstances)
        }
      }
    }
    cache.getListenable.addListener(listener)
  }
}
