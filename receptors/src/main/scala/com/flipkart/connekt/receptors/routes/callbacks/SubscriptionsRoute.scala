/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.receptors.routes.callbacks

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.{SubscriptionAction, Subscription}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.SubscriptionService
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

import scala.util.{Failure, Success}
import com.flipkart.connekt.commons.utils.StringUtils._

class SubscriptionsRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route = pathPrefix("v1" / "subscription") {
    authenticate { user =>
      pathEndOrSingleSlash {
        post {
          entity(as[Subscription]) { subscription =>
            subscription.createdBy = user.userId
            SubscriptionService.add(subscription) match {
              case Success(id) =>
                subscription.id = id
                complete(GenericResponse(StatusCodes.Created.intValue, null, Response("Subscription created", subscription)))
              case Failure(e) => complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Subscription creation failed: " + e, null)))
            }
          }
        }
      } ~
        pathPrefix(Segment) {
          (subscriptionId: String) =>
            post {
              entity(as[Subscription]) { subscription =>
                subscription.createdBy = user.userId
                subscription.id = subscriptionId
                SubscriptionService.update(subscription) match {
                  case Success(result) => complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription updated", subscription)))
                  case Failure(e) if e.getMessage.contains("No Subscription found") => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription updation failed: " + e, null)))
                  case Failure(e) => complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Subscription updation failed: " + e, null)))
                }
              }
            } ~
              get {
                pathEndOrSingleSlash {
                  SubscriptionService.get(subscriptionId) match {
                    case Success(subscription) =>
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription fetched", subscription)))
                    case Failure(e) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription fetching failed: " + e, null)))
                  }
                } ~
                  path("start") {
                    SubscriptionService.get(subscriptionId) match {
                      case Success(sub) => sub match {
                        case Some(subscription) =>
                          SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION, List(SubscriptionAction.START.toString, subscription.getJson)))
                          complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription started successfully", subscription)))
                        case None => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription starting failed: No such subscription found", null)))
                      }
                      case Failure(e) => complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Subscription starting failed: " + e, null)))
                    }
                  } ~
                  path("stop") {
                    SubscriptionService.get(subscriptionId) match {
                      case Success(sub) => sub match {
                        case Some(subscription) =>
                          SyncManager.get().publish(SyncMessage(topic = SyncType.SUBSCRIPTION, List(SubscriptionAction.STOP.toString, subscription.getJson)))
                          complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription stopped successfully", subscription)))
                        case None => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription stopping failed: No such subscription found", null)))
                      }
                      case Failure(e) => complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Subscription stopping failed: " + e, null)))
                    }
                  }
              } ~
              delete {
                SubscriptionService.remove(subscriptionId) match {
                  case Success(code) => complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Subscription deleted successfully", null)))
                  case Failure(e) if e.getMessage.contains("No Subscription found") => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Subscription deletion failed: " + e, null)))
                  case Failure(e) => complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Subscription deletion failed: " + e, null)))
                }
              }
        }
    }
  }
}
