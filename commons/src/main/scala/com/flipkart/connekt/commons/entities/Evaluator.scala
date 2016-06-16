package com.flipkart.connekt.commons.entities

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.flipkart.connekt.commons.entities.fabric.GroovyFabric
import com.flipkart.connekt.commons.iomodels.{CallbackEvent, CardsRequestInfo, PNRequestInfo}
import fkint.mp.connekt.PNCallbackEvent

/**
  * Created by harshit.sinha on 07/06/16.
  */

trait Evaluator {

  def evaluate(event : CallbackEvent) : Boolean

}
