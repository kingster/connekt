/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities.bigfoot

import com.fasterxml.jackson.annotation.JsonIgnore
import com.flipkart.seraph.schema.BaseSchema

abstract class EntityBaseSchema extends BaseSchema {

  @JsonIgnore
  def id: String

}
