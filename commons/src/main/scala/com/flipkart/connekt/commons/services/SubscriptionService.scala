package com.flipkart.connekt.commons.services

import java.util.{Date, UUID}

import com.flipkart.connekt.commons.cache.{LocalCacheManager, LocalCacheType}
import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.{Subscription, SubscriptionRequest}

import scala.util.{Failure, Success, Try}
import com.flipkart.connekt.commons.core.Wrappers._


/**
  * Created by harshit.sinha on 09/06/16.
  **/

object SubscriptionService {

  val invoker = DaoFactory.getSubscriptionDao

  private def cacheKey(sId: String, createdBy:String) = sId+createdBy

  def getSubscription(sId: String, createdBy: String): Try[Option[Subscription]] =  Try_ {
      LocalCacheManager.getCache(LocalCacheType.Subscription).get(cacheKey(sId,createdBy)).orElse {
        val optionSubscription= invoker.getSubscription(sId, createdBy)
        optionSubscription match {
          case Some(subscription) =>
            LocalCacheManager.getCache(LocalCacheType.Subscription).put(cacheKey(sId, createdBy), optionSubscription.get)
            optionSubscription
          case  None => None
        }
    }
  }

  def writeSubscription(subscriptionRequest: SubscriptionRequest, userId: String) : Try[Option[Subscription]] = Try_ {

    val subscription = new Subscription()
    subscription.sId= UUID.randomUUID().toString
    subscription.sName = subscriptionRequest.sName
    subscription.endpoint = subscriptionRequest.endpoint
    subscription.createdBy = userId
    subscription.createdTS= new Date()
    subscription.lastUpdatedTS = new Date()
    subscription.groovyString = subscriptionRequest.groovyString
    subscription.shutdownThreshold = subscriptionRequest.shutdownThreshold

    val optionSubscription= invoker.writeSubscription(subscription)
    optionSubscription match {
      case Some(subscription) =>
        LocalCacheManager.getCache(LocalCacheType.Subscription).put(cacheKey(subscription.sId,subscription.createdBy), subscription)
        optionSubscription
      case None => None
    }
  }

  def writeSubscription(subscription: Subscription) :  Try[Option[Subscription]] = Try_ {
    val optionSubscription = invoker.writeSubscription(subscription)
    optionSubscription match {
      case Some(subscription) =>
        LocalCacheManager.getCache(LocalCacheType.Subscription).put(cacheKey(subscription.sId, subscription.createdBy), subscription)
        optionSubscription
      case None => None
    }
  }

  def updateSubscription(subscriptionRequest: SubscriptionRequest, sId: String, createdBy: String): Try[Option[Subscription]] =  {

    val subscriptionExisting = getSubscription(sId, createdBy)
    subscriptionExisting match {
      case Success(optionSubscription) =>
        optionSubscription match {
          case Some(subscription) =>
            subscription.endpoint = subscriptionRequest.endpoint
            subscription.groovyString = subscriptionRequest.groovyString
            subscription.lastUpdatedTS= new Date()
            writeSubscription(subscription)
          case None => Try(None)
        }
      case Failure(e) => throw e
    }
  }

  def deleteSubscription(sId: String, createdBy: String): Try[Boolean] = Try_ {
    val subscriptionExisting = getSubscription(sId, createdBy)
    subscriptionExisting match {
      case Success(optionSubscription) =>
        optionSubscription match {
          case Some(subscription) =>
            LocalCacheManager.getCache(LocalCacheType.Subscription).remove(cacheKey(sId, createdBy))
            invoker.deleteSubscription(sId)
            true
          case None => throw new Exception()
        }
      case Failure(e) => throw e
    }
  }


}
