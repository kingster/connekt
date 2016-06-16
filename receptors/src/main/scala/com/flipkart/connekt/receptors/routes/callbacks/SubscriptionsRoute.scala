package com.flipkart.connekt.receptors.routes.callbacks

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.SubscriptionRequest
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}

import scala.util.{Failure, Success}

case class ActionRequested(action: String)
class SubscriptionsRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route = pathPrefix("v1" / "subscription") {
    authenticate { user =>
      pathEndOrSingleSlash {
        post {
          entity(as[SubscriptionRequest]) { e =>
            SubscriptionService.writeSubscription(e, user.userId) match {
              case Success(optionSubscription) =>
                optionSubscription match {
                  case Some(subscription) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("subscription created", subscription)))
                  case None => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("subscription failed", null)))
                }
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("subscription failed", null)))
            }
          }
        }
      } ~
      pathPrefix(Segment) { (sId: String) =>
        post {
          entity(as[SubscriptionRequest]) { e =>
            SubscriptionService.updateSubscription(e, sId, user.userId) match {
              case Success(optionSubscription) =>
                optionSubscription match {
                  case Some(subscription) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("updating successful", subscription)))
                  case None => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("updating failed", null)))
                }
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("updating failed", null)))
            }
          }
        } ~
        get {
          pathEndOrSingleSlash {
            SubscriptionService.getSubscription(sId, user.userId) match {
              case Success(optionSubscription) =>
                optionSubscription match {
                  case Some(subscription) => complete(GenericResponse(StatusCodes.OK.intValue, null, Response("fetch successful", subscription)))
                  case None => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("fetch failed", null)))
                }
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("fetch failed", null)))
            }
          } ~
          path("start") {
            SubscriptionService.getSubscription(sId, user.userId) match {
              case Success(optionSubscription) =>
                optionSubscription match {
                  case Some(subscription) =>
                    SyncManager.get.publish(SyncMessage(topic = SyncType.SUBSCRIPTION_REQUEST, List("start", subscription)))
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("start successful", subscription)))
                  case None => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("start failed", null)))
                }
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("start failed", null)))
            }
          } ~
          path("stop") {
            SubscriptionService.getSubscription(sId, user.userId) match {
              case Success(optionSubscription) =>
                optionSubscription match {
                  case Some(subscription) =>
                    SyncManager.get.publish(SyncMessage(topic = SyncType.SUBSCRIPTION_REQUEST, List("stop", subscription)))
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("stop successful", subscription)))
                  case None => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("stop failed", null)))
                }
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("stop failed", null)))
            }
          }
        } ~
        delete {
          SubscriptionService.deleteSubscription(sId, user.userId) match {
            case Success(code) =>
              complete(GenericResponse(StatusCodes.OK.intValue, null, Response("deletion successful", null)))
            case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("deletion failed", null)))
          }
        }
      }
    }
  }
}
