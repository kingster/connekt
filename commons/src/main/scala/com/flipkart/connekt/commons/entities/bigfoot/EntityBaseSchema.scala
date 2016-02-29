package com.flipkart.connekt.commons.entities.bigfoot

import com.fasterxml.jackson.annotation.JsonIgnore
import com.flipkart.seraph.schema.BaseSchema

/**
 * Created by nidhi.mehla on 29/02/16.
 */
abstract class EntityBaseSchema extends BaseSchema {

  @JsonIgnore
  def id: String

}