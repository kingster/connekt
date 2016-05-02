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
package com.flipkart.connekt.receptors.routes.stencils

import java.util.Date

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.{Bucket, Stencil}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

import scala.util.{Failure, Success}

class StencilsRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  val route =
    authenticate {
      user =>
        pathPrefix("v1") {
          pathPrefix("stencils") {
            path("bucket" / Segment) {
              (name: String) =>
                post {
                  meteredResource("stencilCreateBucket") {
                    authorize(user, "ADMIN_BUCKET") {
                      val bucket = new Bucket
                      bucket.name = name
                      val id = "STENBUC_" + StringUtils.generateRandomStr(4)
                      bucket.id = id
                      StencilService.addBucket(bucket) match {
                        case Success(x) =>
                          complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"StencilBucket created for name: ${bucket.name}", Map("id" -> id))))
                        case Failure(e) =>
                          complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"StencilBucket creation failed for name: ${bucket.name}", e)))
                      }
                    }
                  }
                } ~ get {
                  meteredResource("stencilGetBucket") {
                    authorize(user, "ADMIN_BUCKET") {
                      StencilService.getBucket(name) match {
                        case Some(bucket) =>
                          complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"StencilBucket found for name: ${bucket.name}", Map("bucket" -> bucket))))
                        case _ =>
                          complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"StencilBucket not found for name: $name}", null)))
                      }
                    }
                  }
                }
            } ~ path("bucket" / "touch" / Segment) {
              (id: String) =>
                post {
                  meteredResource("stencilTouchBucket") {
                    SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_BUCKET_CHANGE, List(id)))
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Triggered  Change for client: $id", null)))
                  }
                }
            } ~ pathPrefix(Segment) {
              (id: String) =>
                StencilService.get(id) match {
                  case Some(stencil) =>
                    val bucketIds = stencil.bucket.split(",")
                    path("preview") {
                      post {
                        meteredResource("stencilPreviewHead") {
                          entity(as[ObjectNode]) { entity =>
                            authorize(user, bucketIds.map("STENCIL_PREVIEW_" + _): _*) {
                              val channelReqData = StencilService.render(stencil, entity)
                              complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("channelRequest" -> channelReqData))))
                            }
                          }
                        }
                      }
                    } ~ pathPrefix(Segment) {
                      (version: String) =>
                        path("preview") {
                          post {
                            meteredResource("stencilPreviewVersion") {
                              entity(as[ObjectNode]) { entity =>
                                authorize(user, bucketIds.map("STENCIL_PREVIEW_" + _): _*) {
                                  StencilService.get(id, Some(version)) match {
                                    case Some(stn) =>
                                      val channelRequest = StencilService.render(stn, entity)
                                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("channelRequest" -> channelRequest))))
                                    case None =>
                                      complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencils Not found for id: $id", null)))
                                  }
                                }
                              }
                            }
                          }
                        }  ~ path("touch") {
                          post {
                            meteredResource("stencilTouch") {
                              SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_CHANGE, List(id, version)))
                              complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Triggered  Change for client: $id", null)))
                            }
                          }
                        } ~ pathEndOrSingleSlash {
                          get {
                            meteredResource("stencilGetVersion") {
                              authorize(user, bucketIds.map("STENCIL_GET_" + _): _*) {
                                StencilService.get(id, Option(version)) match {
                                  case Some(stnc) =>
                                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("stencils" -> stencil))))
                                  case None =>
                                    complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil not found for name: $id with version: $version", null)))
                                }
                              }
                            }
                          }
                        }
                    } ~ pathEndOrSingleSlash {
                      put {
                        meteredResource("stencilUpdate") {
                          authorize(user, bucketIds.map("STENCIL_UPDATE_" + _): _*) {
                            entity(as[Stencil]) { stnc =>
                              stnc.createdBy = stencil.createdBy
                              stnc.creationTS = stencil.creationTS
                              stnc.lastUpdatedTS = new Date(System.currentTimeMillis())
                              stnc.id = id
                              stnc.version = stencil.version + 1
                              stnc.updatedBy = user.userId
                              stnc.bucket = stencil.bucket.split(",").map(StencilService.getBucket(_).map(_.id.toUpperCase).getOrElse("")).filter(_ != "").mkString(",")
                              StencilService.update(stnc) match {
                                case Success(sten) =>
                                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencil updated for id: $id", null)))
                                case _ =>
                                  complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil for id: ${stencil.id}", null)))
                              }
                            }
                          }
                        }
                      } ~ get {
                        meteredResource("stencilGet") {
                          authorize(user, bucketIds.map("STENCIL_GET_" + _): _*) {
                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("stencils" -> stencil))))
                          }
                        }
                      }
                    }
                  case None =>
                    complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil not found for name: $id", null)))

                }
            } ~ pathEndOrSingleSlash {
              post {
                meteredResource("stencilAdd") {
                  entity(as[Stencil]) { stencil =>
                    val bucketIds = stencil.bucket.split(",").map(StencilService.getBucket(_).map(_.id.toUpperCase).getOrElse("")).filter(_ != "")
                    val resources = bucketIds.map("STENCIL_UPDATE_" + _)
                    authorize(user, resources: _*) {
                      stencil.bucket = bucketIds.mkString(",")
                      stencil.id = "STNC" + StringUtils.generateRandomStr(4)
                      stencil.createdBy = user.userId
                      stencil.updatedBy = user.userId
                      stencil.version = 1
                      stencil.creationTS = new Date(System.currentTimeMillis())
                      stencil.lastUpdatedTS = new Date(System.currentTimeMillis())

                      StencilService.add(stencil) match {
                        case Success(sten) =>
                          complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Stencil registered with id: ${stencil.id}", Map("id" -> stencil.id))))
                        case Failure(e) =>
                          complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil for id: ${stencil.id}, e: ${e.getMessage}", null)))
                      }
                    }
                  }
                }
              }
            }
          }
        }
    }
}
