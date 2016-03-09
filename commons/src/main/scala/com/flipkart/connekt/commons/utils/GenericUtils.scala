package com.flipkart.connekt.commons.utils

import reflect.runtime.universe._
import reflect.ClassTag

/**
 * Created by kinshuk.bairagi on 10/03/16.
 */
object GenericUtils {

  def typeToClassTag[T: TypeTag]: ClassTag[T] = {
    ClassTag[T]( typeTag[T].mirror.runtimeClass( typeTag[T].tpe ) )
  }

}
