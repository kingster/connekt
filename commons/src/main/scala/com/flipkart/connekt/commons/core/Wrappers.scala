/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.core

import com.flipkart.connekt.commons.factories.LogFile.LogFile
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}

import scala.util.{Failure, Success, Try}

object Wrappers {

  def Try_#[T](fileName: LogFile = LogFile.SERVICE, message: String = "ERROR")(f: => T) : Try[T] = {
    try {
      Success(f)
    }
    catch {
      case e: Throwable =>
        ConnektLogger(fileName).error(s"$message, e: ${e.getMessage}",e)
        Failure(e)
    }
  }

  def Try_ [T](f: => T) : Try[T] = {
    Try_#()(f)
  }
}
