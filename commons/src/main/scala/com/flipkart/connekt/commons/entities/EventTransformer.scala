package com.flipkart.connekt.commons.entities

import java.util.Date

/**
  * Created by harshit.sinha on 27/06/16.
  */
class EventTransformer {

  var headerTx : AnyRef = _
  var bodyTx : AnyRef = _

  def this(headerTx: AnyRef, bodyTx: AnyRef) = {
    this
    this.headerTx = headerTx
    this.bodyTx = bodyTx
  }

  override def toString =
    s"EventTranformer($headerTx, $bodyTx)"
}
