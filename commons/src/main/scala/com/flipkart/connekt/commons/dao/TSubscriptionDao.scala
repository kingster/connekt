package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.Subscription
import com.sun.xml.internal.ws.api.server.HttpEndpoint

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by harshit.sinha on 08/06/16.
  */
trait TSubscriptionDao {

  def get(sId: String) : Option[Subscription]
  def add(subscription: Subscription) : Unit
  def delete(sId: String): Unit
}

