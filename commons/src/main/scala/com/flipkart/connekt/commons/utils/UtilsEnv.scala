package com.flipkart.connekt.commons.utils

/**
 *
 *
 * @author durga.s
 * @version 11/15/15
 */
object UtilsEnv {

  def getConfEnv = Option(System.getenv("CONNEKT_ENV")).getOrElse("local")
}
