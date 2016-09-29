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
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils
import org.apache.curator.x.discovery.details.JsonInstanceSerializer
import org.apache.curator.x.discovery.{ServiceDiscovery, ServiceDiscoveryBuilder, ServiceInstance, ServiceProvider}
import org.codehaus.jackson.map.annotate.JsonRootName

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class ServiceHostsDiscovery(zkPath: String, client: CuratorFramework) {

  val serializer = new JsonInstanceSerializer[InstanceDetails](classOf[InstanceDetails])
  private val serviceDiscovery: ServiceDiscovery[InstanceDetails] = ServiceDiscoveryBuilder.builder(classOf[InstanceDetails])
    .client(client)
    .basePath(zkPath)
    .serializer(serializer)
    .build()

  val providers: Map[String, ServiceProvider[InstanceDetails]] = Map()
  try {
    serviceDiscovery.start()
  } catch {
    case e: Exception =>
      ConnektLogger(LogFile.SERVICE).error(s"Failed to Start Discovery ServiceHostsDiscovery", e)
  }

  def listInstances(serviceName: String): List[String] = {
    serviceDiscovery.queryForInstances(serviceName).iterator().map(instance => instance.getId).toList
  }

  def outputInstance(instance: ServiceInstance[InstanceDetails]) {
    val serviceNames = serviceDiscovery.queryForNames()
    ConnektLogger(LogFile.CLIENTS).info(s"Name of instances in $zkPath = $serviceNames ")
  }

  def close() {
    CloseableUtils.closeQuietly(serviceDiscovery)
    providers.values.foreach(CloseableUtils.closeQuietly(_))
    CloseableUtils.closeQuietly(client)
  }

  def deleteInstance(serviceName: String, id: String) {
    val server = new ConnektInstance(zkPath, client, serviceName, id)
    CloseableUtils.closeQuietly(server)
  }

  def addInstance(serviceName: String, id: String) {
    val server = new ConnektInstance(zkPath, client, serviceName, id)
    server.start()
    ConnektLogger(LogFile.CLIENTS).info(s"Instance $serviceName added to path $zkPath")
  }
}

@JsonRootName("details")
class InstanceDetails(@BeanProperty var description: String) {
  def this() = this("")
}
