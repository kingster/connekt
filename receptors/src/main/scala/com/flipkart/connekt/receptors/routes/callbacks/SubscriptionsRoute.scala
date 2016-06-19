package com.flipkart.connekt.receptors.routes.callbacks

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Subscription
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}

import scala.util.{Failure, Success}

/**
  * Created by harshit.sinha on 07/06/16.
  */
class SubscriptionsRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route = pathPrefix("v1" / "subscription") {
    authenticate { user =>
      pathEndOrSingleSlash {
        post {
          entity(as[Subscription]) { sub =>
            sub.createdBy = user.userId
            SubscriptionService.add(sub) match {
              case Success(createdSubscription) => complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription created", createdSubscription)))
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription creation failed: " + e, null)))
            }
          }
        }
      } ~
      pathPrefix(Segment) { (id: String) =>
        post {
          entity(as[Subscription]) { sub =>
            sub.createdBy = user.userId
            SubscriptionService.update(sub, id) match {
              case Success(updatedSubscription) => complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription updated", updatedSubscription)))
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription updation failed: "+ e, null)))
            }
          }
        } ~
        get {
          pathEndOrSingleSlash {
            SubscriptionService.get(id) match {
              case Success(fetchedSubscription) => complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription fetched", fetchedSubscription)))
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription fetching failed: "+ e, null)))
            }
          } ~
          path("start") {
            SubscriptionService.get(id) match {
              case Success(subscription) =>
                SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION_REQUEST, List("start", subscription)))
                complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription started successfully", subscription)))
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription starting failed: "+ e, null)))
            }
          } ~
          path("stop") {
            SubscriptionService.get(id) match {
              case Success(subscription) =>
                SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION_REQUEST, List("stop", subscription)))
                complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription stopped successfully", subscription)))
              case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription stopping failed: "+ e, null)))
            }
          }
        } ~
        delete {
          SubscriptionService.remove(id) match {
            case Success(code) => complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription deleted successfully", null)))
            case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subsciption deletion failed: "+ e, null)))
          }
        }
      }
    }
  }
}
