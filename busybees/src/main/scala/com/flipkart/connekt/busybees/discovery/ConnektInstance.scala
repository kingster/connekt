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

import java.io.{Closeable, IOException}

import com.flipkart.utils.NetworkUtils
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils
import org.apache.curator.x.discovery.details.JsonInstanceSerializer
import org.apache.curator.x.discovery.{ServiceDiscoveryBuilder, ServiceInstance, UriSpec}

protected class ConnektInstance(zkPath: String, client: CuratorFramework, serviceName: String, id: String) extends Closeable {

  private val serializer: JsonInstanceSerializer[InstanceDetails] = new JsonInstanceSerializer[InstanceDetails](classOf[InstanceDetails])
  private val thisInstance  = ServiceInstance.builder[InstanceDetails]
    .name(serviceName)
    .id(id)
    .address(NetworkUtils.getIPAddress)
    .payload(new InstanceDetails(id))
    .build

  private val serviceDiscovery  = ServiceDiscoveryBuilder.builder(classOf[InstanceDetails])
    .client(client)
    .basePath(zkPath)
    .serializer(serializer)
    .thisInstance(thisInstance)
    .build

  def getThisInstance: ServiceInstance[InstanceDetails] = thisInstance

  @throws[Exception]
  def start() {
    serviceDiscovery.start()
  }

  @throws[IOException]
  def close() {
    CloseableUtils.closeQuietly(serviceDiscovery)
  }
}
