package com.flipkart.connekt.commons.utils

import org.joda.time.format.DateTimeFormat

/**
 * Created by nidhi.mehla on 08/02/16.
 */
object DateTimeUtils {

  val sdf = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss")

  def getStandardFormatted(epoch:Long = System.currentTimeMillis()) = sdf.print(epoch)
}
