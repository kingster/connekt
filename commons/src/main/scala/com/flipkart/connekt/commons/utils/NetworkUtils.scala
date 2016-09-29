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
package com.flipkart.connekt.commons.utils

import java.io.{BufferedReader, InputStreamReader}
import java.net.{Inet6Address, NetworkInterface}
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._


object NetworkUtils {

  lazy val logger = LoggerFactory.getLogger(NetworkUtils.getClass)

  def getHostname: String = try {
    var hostName: String = java.net.InetAddress.getLocalHost.getHostName
    if (hostName.contains("local"))
      hostName = "localhost" // override name for local!
    else
      hostName = java.net.InetAddress.getLocalHost.getCanonicalHostName

    if (!hostName.equalsIgnoreCase("localhost") && !hostName.contains(".")) {
      logger.debug("Falling back to java sys execute! this must not happen")

      val rt: Runtime = Runtime.getRuntime
      val p = rt.exec("hostname -f")
      val stdInput: BufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream))
      hostName = stdInput.readLine()
    }
    hostName
  } catch {
    case e: Exception =>
      logger.error("Get Hostname Exception", e)
      "localhost"
  }

  def getIPAddress: String = {
    var ip = "127.0.0.1"

    try {
      NetworkInterface.getNetworkInterfaces.asScala.foreach(networkInterface => {
        networkInterface.getInetAddresses.asScala.
          filter(inetAddress =>
          !inetAddress.isLoopbackAddress
            && !inetAddress.isMulticastAddress
            && !inetAddress.isInstanceOf[Inet6Address])
          .foreach(address => {
          ip = address.getHostAddress
        })
      })
    } catch {
      case e: Exception =>
        logger.error("Get IP Exception", e)
    }

    /**
     * Still 127.0.0.1, since didn't get from above. Shouldn't happen ideally.
     */
    if ("127.0.0.1".equalsIgnoreCase(ip)) {
      try {
        logger.error("getIPAddress Falling back to java sys execute!")
        val rt: Runtime = Runtime.getRuntime
        val p = rt.exec("hostname -i")
        val stdInput: BufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream))
        val sysIp = stdInput.readLine()
        if (sysIp != null && sysIp.nonEmpty)
          ip = sysIp
      } catch {
        case e: Exception =>
          logger.error("Get IP Address from CommandLine", e)
      }
    }

    ip
  }
}
