package com.flipkart.connekt.commons.utils

import java.io.{BufferedReader, InputStreamReader}
import java.net.{Inet6Address, NetworkInterface, URL}

import com.flipkart.connekt.commons.factories.{LogFile, ConnektLogger}

import scala.collection.JavaConverters._


object NetworkUtils {

  def getHostname: String = try {
    var hostName: String = java.net.InetAddress.getLocalHost.getHostName
    if (hostName.contains("local"))
      hostName = "localhost" // override name for local!
    else
      hostName = java.net.InetAddress.getLocalHost.getCanonicalHostName

    if(!hostName.equalsIgnoreCase("localhost") && !hostName.contains(".")){
      ConnektLogger(LogFile.SERVICE).debug("Falling back to java sys execute! this must not happen")

      val  rt:Runtime = Runtime.getRuntime
      val p = rt.exec("hostname -f")
      val stdInput: BufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream))
      hostName = stdInput.readLine()
    }
    hostName
  } catch {
    case e: Exception =>
      ConnektLogger(LogFile.SERVICE).error("Get Hostname Exception", e)
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
        ConnektLogger(LogFile.SERVICE).error("Get IP Exception", e)
    }

    /**
     * Still 127.0.0.1, since didn't get from above. Shouldn't happen ideally.
     */
    if("127.0.0.1".equalsIgnoreCase(ip)) {
      try {
        ConnektLogger(LogFile.SERVICE).error("getIPAddress Falling back to java sys execute!")
        val rt: Runtime = Runtime.getRuntime
        val p = rt.exec("hostname -i")
        val stdInput: BufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream))
        val sysIp = stdInput.readLine()
        if (!StringUtils.isNullOrEmpty(sysIp))
          ip = sysIp
      } catch {
        case e: Exception =>
          ConnektLogger(LogFile.SERVICE).error("Get IP Address from CommandLine", e)
      }
    }

    ip
  }



}
