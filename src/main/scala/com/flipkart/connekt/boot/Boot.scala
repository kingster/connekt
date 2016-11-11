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
package com.flipkart.connekt.boot

import com.flipkart.connekt.busybees.BusyBeesBoot
import com.flipkart.connekt.firefly.FireflyBoot
import com.flipkart.connekt.receptors.ReceptorsBoot
import com.flipkart.connekt.barklice.BarkLiceBoot
import com.flipkart.connekt.commons.utils.NetworkUtils

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
        sys.addShutdownHook(ReceptorsBoot.terminate())
        ReceptorsBoot.start()

      case "busybees" =>
        sys.addShutdownHook(BusyBeesBoot.terminate())
        BusyBeesBoot.start()

      case "firefly" =>
        sys.addShutdownHook(FireflyBoot.terminate())
        FireflyBoot.start()

      case "barklice" =>
        sys.addShutdownHook(BarkLiceBoot.terminate())
        BarkLiceBoot.start

      case _ =>
        println(usage)
    }
  }

  private def usage: String = {
    """
      |Invalid Command. See Usage
      |
      |Usage : AppName (receptors | busybees | firefly)
    """.stripMargin

  }
}
