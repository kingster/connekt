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
package com.flipkart.connekt.busybees.tests.streams

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}
import com.flipkart.connekt.busybees.streams.StageSupervision
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class TopologyUTSpec extends CommonsBaseTest {

  implicit val system = ActorSystem("Test")
  implicit val ec = system.dispatcher

  val settings = ActorMaterializerSettings(system)
    .withDispatcher("akka.actor.default-dispatcher")
    .withAutoFusing(enable = false) //TODO: Enable async boundaries and then enable auto-fusing
    .withSupervisionStrategy(StageSupervision.decider)

  implicit val mat = ActorMaterializer(settings)

}
