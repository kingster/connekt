package com.flipkart.connekt.commons.services

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.Subscription

import scala.util.{Failure, Try}
import com.flipkart.connekt.commons.core.Wrappers._

object SubscriptionService {

  lazy val dao = DaoFactory.getSubscriptionDao

  def add(subscription: Subscription): Try[String] = Try_#(message = "SubscriptionService.add failed") {
    subscription.id = UUID.randomUUID().toString
    dao.add(subscription)
    LocalCacheManager.getCache(LocalCacheType.Subscription).put[Subscription](subscription.id, subscription)
    subscription.id
  }

  def get(id: String): Try[Option[Subscription]] = Try_#(message = "SubscriptionService.get failed") {
    LocalCacheManager.getCache(LocalCacheType.Subscription).get[Subscription](id).orElse {
      dao.get(id) match {
        case Some(subscription) =>
          LocalCacheManager.getCache(LocalCacheType.Subscription).put[Subscription](subscription.id, subscription)
          Some(subscription)
        case None => None
      }
    }
  }

  def update(subscription: Subscription): Try[Boolean] = {
    subscription.lastUpdatedTS = new Date(System.currentTimeMillis())
    get(subscription.id).flatMap {
      case Some(subscription) => Try_#(message = "SubscriptionService.update failed") {
        dao.add(subscription)
        LocalCacheManager.getCache(LocalCacheType.Subscription).put[Subscription](subscription.id, subscription)
        true
      }
      case None => Failure(new Throwable(s"No Subscription found for id: [${subscription.id}] to update."))
    }
  }

  def remove(id: String): Try[Boolean] = {
    get(id).flatMap {
      case Some(subscription) => Try_#(message = "SubscriptionService.delete failed") {
        dao.delete(id)
        LocalCacheManager.getCache(LocalCacheType.Subscription).remove(id)
        true
      }
      case None => Failure(new Throwable(s"No Subscription found for id: [$id] to delete."))
    }
  }

}
