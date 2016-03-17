/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.utils

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object GenericUtils {

  def typeToClassTag[T: TypeTag]: ClassTag[T] = {
    ClassTag[T]( typeTag[T].mirror.runtimeClass( typeTag[T].tpe ) )
  }



}
