package com.flipkart.connekt.receptors.tests.routes.reports

import java.util.Calendar

import akka.http.scaladsl.model.StatusCodes
import com.flipkart.connekt.commons.utils.DateTimeUtils
import com.flipkart.connekt.receptors.routes.reports.ReportsRoute
import com.flipkart.connekt.receptors.tests.routes.BaseRouteTest

class ReportsRouteTest extends BaseRouteTest {

  val date = DateTimeUtils.calenderDate.print(Calendar.getInstance().getTimeInMillis)
  var reportRoute = new ReportsRoute().route

  "ReportRoute GET test " should "return OK" in {
    Get(s"/v1/reports/date/$date?clientId=something").addHeader(header) ~> reportRoute ~>
      check {
        status shouldEqual StatusCodes.OK
      }
  }

}
