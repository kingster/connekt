package com.flipkart.connekt.receptors.routes.pull

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.flipkart.connekt.commons.entities.Channel
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile, ServiceFactory}
import com.flipkart.connekt.commons.iomodels.MessageStatus.InternalStatus
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.directives.MPlatformSegment
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

class PullRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {
  lazy implicit val stencilService = ServiceFactory.getStencilService
  val route =
    authenticate {
      appUser =>
        pathPrefix("v1") {
          pathPrefix("pull" / "inapp") {
            path(MPlatformSegment / Segment / Segment) {
              (platform: MobilePlatform, appName: String, instanceId: String) =>
                authorize(appUser, "FETCH", s"FETCH_$appName") {
                  get {
                    meteredResource(s"fetch.$platform.$appName") {
                      parameters('startTs.as[Long], 'endTs ? System.currentTimeMillis, 'skipIds.*) { (startTs, endTs, skipIds) =>
                        require(startTs < endTs, "startTs must be prior to endTs")

                        val requestEvents = ServiceFactory.getCallbackService.fetchCallbackEventByContactId(s"${appName.toLowerCase}$instanceId", Channel.PULL, startTs + 1, endTs)
                        val messageService = ServiceFactory.getPullMessageService

                        val messages: Try[List[ConnektRequest]] = requestEvents.map(res => {
                          val messageIds: List[String] = res.map(_._1.asInstanceOf[PullCallbackEvent]).map(_.messageId).distinct
                          val fetchedMessages: Try[List[ConnektRequest]] = messageService.getRequestInfo(messageIds)
                          fetchedMessages.map(_.filter(_.expiryTs.map(_ >= System.currentTimeMillis).getOrElse(true))).getOrElse(List.empty[ConnektRequest])
                        })

                        //Coalsecing the sheit
                        val pullStencil = stencilService.getStencilsByName("ckt-retailapp-pull").find(_.component == "coal")

                        val stencilResult = stencilService.materialize(pullStencil.get, messages.getJson.getObj[ObjectNode])

                        val iterator = stencilResult.asInstanceOf[ObjectNode].get("value").iterator()
                        var connektRequestsTransformed: List[ConnektRequest] = List.empty
                        while(iterator.hasNext){
                          val something = iterator.next()
                          connektRequestsTransformed ::= something.getJson.getObj[ConnektRequest]
                        }


                        val pullRequests = connektRequestsTransformed.map(r =>
                          r.id -> (Option(r.channelData) match {
                            case Some(cD) => cD
                            case None => r.getComputedChannelData
                          })
                        ).toMap

                        val finalTs = requestEvents.getOrElse(List.empty[(CallbackEvent, Long)]).map(_._2).reduceLeftOption(_ max _).getOrElse(endTs)

                        complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Fetched result for $instanceId", pullRequests)))
                      }
                    }
                  }
                }
            } ~ path(Segment) {
              (appName: String) =>
                authorize(appUser, "PULL", s"PULL_$appName") {
                  post {
                    meteredResource(s"sendUserPull.$appName") {
                      getXHeaders { headers =>
                        entity(as[ConnektRequest]) { r =>
                          val request = r.copy(clientId = appUser.userId, channel = "pull", meta = {
                            Option(r.meta).getOrElse(Map.empty[String, String]) ++ headers
                          })
                          request.validate

                          ConnektLogger(LogFile.SERVICE).debug(s"Received Pull request: ${request.toString}")

                          val pullRequestInfo = request.channelInfo.asInstanceOf[PullRequestInfo].copy(appName = appName.toLowerCase)

                          if (pullRequestInfo.userIds != null && pullRequestInfo.userIds.nonEmpty) {
                            val groupedPlatformRequests = ListBuffer[ConnektRequest]()
                            groupedPlatformRequests += request.copy(channelInfo = pullRequestInfo.copy())

                            val failure = ListBuffer[String]()
                            val success = scala.collection.mutable.Map[String, Set[String]]()
                            val createEvents = ListBuffer[PullCallbackEvent]()
                            if (groupedPlatformRequests.nonEmpty) {
                              groupedPlatformRequests.foreach { p =>
                                ServiceFactory.getPullMessageService.saveRequestToHbase(p) match {
                                  case Success(id) =>
                                    val userIds = p.channelInfo.asInstanceOf[PullRequestInfo].userIds
                                    success += id -> userIds
                                    ServiceFactory.getReportingService.recordPushStatsDelta(appUser.userId, request.contextId, request.stencilId, Option(p.channelInfo.asInstanceOf[PullRequestInfo].platform), appName, InternalStatus.Received, userIds.size)
                                    userIds.foreach(s =>
                                      //create callback for pull creation
                                      createEvents += PullCallbackEvent(messageId = id, clientId = appUser.userId, userId = s, eventType = InternalStatus.Received, appName = appName, contextId = request.contextId.orNull)
                                    )
                                    ServiceFactory.getCallbackService.persistCallbackEvents(Channel.PULL, createEvents.toList)
                                  case Failure(t) =>
                                    val userIds = p.channelInfo.asInstanceOf[PullRequestInfo].userIds
                                    failure ++= userIds
                                    ServiceFactory.getReportingService.recordPushStatsDelta(appUser.userId, request.contextId, request.stencilId, Option(p.channelInfo.asInstanceOf[PullRequestInfo].platform), appName, InternalStatus.Rejected, userIds.size)
                                }
                              }
                            }

                            complete(GenericResponse(StatusCodes.Created.intValue, null, SendResponse(s"Pull request processed for users", success.toMap, failure.toList)))
                          }
                          else {
                            complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"No users were found .", null)))
                          }
                        }
                      }
                    }
                  }
                }
            } ~ path(Segment / Segment / Segment) {
              (something: String, appName: String, userId: String) =>
                authorize(appUser, "PULL", s"PULL_$appName") {
                  //Delete
                  delete {

                    complete(GenericResponse(StatusCodes.Created.intValue, null, Response("something", "deleted")))
                  }
                }
            }
          }
        }
    }
}
