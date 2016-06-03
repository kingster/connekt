package com.flipkart.connekt.commons.tests.services

import java.util.Calendar

import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.tests.CommonsBaseTest
import com.flipkart.connekt.commons.utils.DateTimeUtils


class ReportingServiceTest extends CommonsBaseTest {

  val date = DateTimeUtils.calenderDate.print(Calendar.getInstance().getTimeInMillis)

  "ReportingService" should "get all report details " in {
    ServiceFactory.getReportingService.getAllDetails(date, "clientId", None, None, None, None) shouldEqual Map()
  }

  "ReportingService" should "adds delta count " in {
    noException should be thrownBy ServiceFactory.getReportingService.recordPushStatsDelta("clientId", None, None, None, "app_name", "random_event", 1)
  }

  "ReportingService" should "get single report details " in {
    ServiceFactory.getReportingService.recordPushStatsDelta("clientId-test", None, None, None, "APP_NAME", null, 10)
    ServiceFactory.getReportingService.statsUpdateTask.run()
    ServiceFactory.getReportingService.getAllDetails(date, "clientId-test", None, None, None, None) shouldEqual Map("push.app_name" -> 10)
  }

}
