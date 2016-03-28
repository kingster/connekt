package com.flipkart.connekt.receptors.service

import java.lang.management.ManagementFactory

/**
 * Created by kinshuk.bairagi on 28/03/16.
 */
object HealthService {

  private [receptors] object ServiceStatus extends Enumeration {
    val IN_ROTATION , OUT_OF_ROTATION = Value
  }

  private var status = ServiceStatus.IN_ROTATION

  def oor(){
    this.status = ServiceStatus.OUT_OF_ROTATION
  }

  def bir(){
    this.status = ServiceStatus.IN_ROTATION
  }
  
  def getStatus:ServiceStatus.Value={
    this.status
  }

  def elbResponse(): ELBResponse ={
    val rb = ManagementFactory.getRuntimeMXBean

    //TODO: Comeup with better implementation of this.
    val capacity:Int = this.status match {
      case ServiceStatus.IN_ROTATION => 100
      case ServiceStatus.OUT_OF_ROTATION => 0
    }

    //TODO: Implement this.
    val requestCount = 0l

    ELBResponse(rb.getUptime,requestCount,capacity)
  }
  

}

private [receptors] case class ELBResponse(uptime:Long, requests:Long, capacity:Int)
