package com.flipkart.connekt.commons.services

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.Subscription

import scala.util.{Failure, Success, Try}
import com.flipkart.connekt.commons.core.Wrappers._


/**
  * Created by harshit.sinha on 09/06/16.
  **/


object SubscriptionService {

  val dao = DaoFactory.getSubscriptionDao

  def add(subscription: Subscription): Try[Subscription] = Try_#(message = "SubscriptionService.add failed") {
    subscription.id = UUID.randomUUID().toString
    if(dao.add(subscription)) {
      LocalCacheManager.getCache(LocalCacheType.Subscription).put[Subscription](subscription.id, subscription)
      subscription
    }
    else throw new Exception(s"No Subscription added for id: [${subscription.id}].")
  }

  def get(id: String): Try[Subscription] = Try_#(message = "SubscriptionService.get failed") {
    val optionSubscription = LocalCacheManager.getCache(LocalCacheType.Subscription).get[Subscription](id)
    optionSubscription match {
      case Some(subscription) =>
        LocalCacheManager.getCache(LocalCacheType.Subscription).put[Subscription](subscription.id, subscription)
        subscription
      case None =>
        dao.get(id) match {
          case Some(subscription) =>
            LocalCacheManager.getCache(LocalCacheType.Subscription).put[Subscription](subscription.id, subscription)
            subscription
          case None => throw new Exception(s"No Subscription found for id: [$id].")
        }
    }
  }

  def update(newSubscription: Subscription, id: String): Try[Subscription] = Try_#(message = "SubscriptionService.update failed") {
    newSubscription.id = id
    newSubscription.lastUpdatedTS = new Date(System.currentTimeMillis())

    val result = get(newSubscription.id)
    result match {
      case Success(subscription) =>
        if(dao.add(newSubscription)) {
          LocalCacheManager.getCache(LocalCacheType.Subscription).put[Subscription](newSubscription.id, newSubscription)
          newSubscription
        }
        else throw new Exception(s"No Subscription added for id: [${newSubscription.id}].")
      case Failure(e) => throw new Exception(s"No Subscription found for id: [$id]. to update")
    }
  }

  def remove(id: String): Try[Boolean] = Try_#(message = "SubscriptionService.delete failed") {
    val result = get(id)
    result match {
      case Success(subscription) =>
        dao.delete(id)
        LocalCacheManager.getCache(LocalCacheType.Subscription).remove(id)
        true
      case Failure(e) => throw new Exception(s"No Subscription found for id: [$id]. to delete")
    }
  }

}
