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
package com.flipkart.connekt.commons.tests.dao

import com.flipkart.connekt.commons.dao.DaoFactory.getStatsReportingDao
import com.flipkart.connekt.commons.tests.CommonsBaseTest

class StatsReportingDaoTest extends CommonsBaseTest {

  val kv: List[(String, Long)] = List()

  "StatsReporting Dao" should "get List" in {
    getStatsReportingDao.get(List()) shouldEqual Map()
  }

  "StatsReporting Dao" should "save Count" in {
    getStatsReportingDao.prefix("not_test") shouldEqual List()
  }

  "StatsReporting Dao" should "put " in {
    noException should be thrownBy getStatsReportingDao.put(List(("test_key", 1L)))
  }

  "StatsReporting Dao" should "counter add delta " in {
    noException should be thrownBy getStatsReportingDao.counter(List(("test_key", 1L)))
  }
}
