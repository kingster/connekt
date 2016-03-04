package com.flipkart.connekt.commons.entities.bigfoot

import com.flipkart.seraph.schema.BaseSchema

/**
 * Created by kinshuk.bairagi on 01/03/16.
 */
trait BigfootSupport[T <: BaseSchema] {

  def toBigfootFormat: T
  
}
