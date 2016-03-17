/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.utils

import org.joda.time.format.DateTimeFormat

object DateTimeUtils {

  val sdf = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss")

  def getStandardFormatted(epoch:Long = System.currentTimeMillis()) = sdf.print(epoch)
}
