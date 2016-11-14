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
package com.flipkart.connekt.commons.tests.sync

import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.sync.SyncType.SyncType
import com.flipkart.connekt.commons.sync.{SyncDelegate, SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.commons.tests.ConnektUTSpec

class SyncManagerTest extends ConnektUTSpec with SyncDelegate {

  "Sync operation" should "sync" in {
    SyncManager.create("127.0.0.1:2181")
    //Subscribe to the NF
    SyncManager.get().addObserver(this, List(SyncType.TEMPLATE_CHANGE))
    SyncManager.get().publish(SyncMessage(SyncType.TEMPLATE_CHANGE, List("Hello via Curator", "Hello via Zookeeper" + System.currentTimeMillis())))
    noException should be thrownBy  SyncTestVariables.lock.await(120, TimeUnit.SECONDS)
    assert(null != SyncTestVariables.receivedData )
  }

  override def onUpdate(_type: SyncType, args: List[AnyRef]): Any = {
    ConnektLogger(LogFile.SERVICE).info("Recieved Async [" + _type + "] with data " + args)
    SyncTestVariables.receivedData = args
    SyncTestVariables.lock.countDown()
  }
}

object SyncTestVariables {

  /** Countdown latch for async testing */
  val lock = new CountDownLatch(1)
  var receivedData: Any = null

}
