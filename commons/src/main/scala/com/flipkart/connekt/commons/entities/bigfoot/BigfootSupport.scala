/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities.bigfoot

import com.flipkart.seraph.schema.BaseSchema

trait BigfootSupport[T <: BaseSchema] {

  def toBigfootFormat: T
  
}
