/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.boot

import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.receptors.ReceptorsBoot
import com.flipkart.utils.NetworkUtils

object Boot extends App {

  println(
    """
      |                                             dP         dP
      |                                             88         88
      |.d8888b. .d8888b. 88d888b. 88d888b. .d8888b. 88  .dP  d8888P
      |88'  `"" 88'  `88 88'  `88 88'  `88 88ooood8 88888"     88
      |88.  ... 88.  .88 88    88 88    88 88.  ... 88  `8b.   88
      |`88888P' `88888P' dP    dP dP    dP `88888P' dP   `YP   dP
    """.stripMargin)

  println("Hello " + NetworkUtils.getHostname + "\n")


  if (args.length < 1) {
    println(usage)
  } else {
    val command = args.head.toString
    command match {
      case "receptors" =>
        sys.addShutdownHook(ReceptorsBoot.terminate)
        ReceptorsBoot.start

      case "busybees" =>
        sys.addShutdownHook(BusyBeesBoot.terminate)
        BusyBeesBoot.start
      case _ =>
        println(usage)
    }
  }


  private def usage: String = {
    """
      |Invalid Command. See Usage
      |
      |Usage : AppName (receptors|busybees)
    """.stripMargin

  }
}
