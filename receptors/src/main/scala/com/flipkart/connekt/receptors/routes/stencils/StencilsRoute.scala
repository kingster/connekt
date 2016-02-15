package com.flipkart.connekt.receptors.routes.stencils

import java.util.Date

import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.{AppUser, Bucket, Stencil}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.util.{Failure, Success}

/**
 * @author aman.shrivastava on 19/01/16.
 */
class StencilsRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler {
  val stencils =
    pathPrefix("v1") {
      pathPrefix("stencils") {
        path("bucket" / Segment) {
          (name: String) =>
            post {
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
            } ~ get {
              authorize(user, "ADMIN_BUCKET") {
                StencilService.getBucket(name) match {
                  case Some(bucket) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"StencilBucket found for name: ${bucket.name}", Map("bucket" -> bucket))))
                  case _ =>
                    complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"StencilBucket not found for name: $name}", null)))
                }
              }
            }
        } ~ pathPrefix(Segment) {
          (id: String) =>
            StencilService.get(id) match {
              case Some(stencil) =>
                val bucketIds = stencil.bucket.split(",")
                path("preview") {
                  post {
                    entity(as[ObjectNode]) { entity =>
                      authorize(user, bucketIds.map("STENCIL_PREVIEW_" + _): _*) {
                        StencilService.render(stencil, entity) match {
                          case Some(channelRequest) =>
                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("channelRequest" -> channelRequest))))
                          case None =>
                            complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencils cannot be render for id: $id", null)))
                        }
                      }
                    }
                  }
                } ~ pathPrefix(Segment) {
                  (version: String) =>
                    path("preview") {
                      post {
                        entity(as[ObjectNode]) { entity =>
                          authorize(user, bucketIds.map("STENCIL_PREVIEW_" + _): _*) {
                            StencilService.get(id, Some(version)) match {
                              case Some(stnc) =>
                                StencilService.render(stnc, entity) match {
                                  case Some(channelRequest) =>
                                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("channelRequest" -> channelRequest))))
                                  case None =>
                                    complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencils cannot be render for id: $id", null)))
                                }
                              case None =>
                                complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencils Not found for id: $id", null)))
                            }
                          }
                        }
                      }
                    } ~ pathEndOrSingleSlash {
                      get {
                        authorize(user, bucketIds.map("STENCIL_GET_" + _): _*) {
                          StencilService.get(id, Option(version)) match {
                            case Some(stnc) =>
                              complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("stencils" -> stencil))))
                            case None =>
                              complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"Stencil not found for name: $id with version: $version", null)))
                          }
                        }
                      }
                    }
                } ~ pathEndOrSingleSlash {
                  put {
                    authorize(user, bucketIds.map("STENCIL_UPDATE_" + _): _*) {
                      entity(as[Stencil]) { stnc =>
                        stnc.id = id
                        stnc.updatedBy = user.userId
                        stnc.bucket = stencil.bucket.split(",").map(StencilService.getBucket(_).map(_.id.toUpperCase).getOrElse("")).filter(_ != "").mkString(",")
                        StencilService.update(stencil) match {
                          case Success(sten) =>
                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencil registered for id: $id", null)))
                          case _ =>
                            complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil for id: ${stencil.id}", null)))
                        }
                      }
                    }
                  } ~ get {
                    authorize(user, bucketIds.map("STENCIL_GET_" + _): _*) {
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("stencils" -> stencil))))
                    }
                  }
                }
              case None =>
                complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response(s"Stencil not found for name: $id", null)))

            }
        } ~ pathEndOrSingleSlash {
          post {
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

                StencilService.add(stencil).get
                complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Stencil registered with id: ${stencil.id}", Map("id" -> stencil.id))))

              }
            }
          }
        }
      }
    }
}
