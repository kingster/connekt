package com.flipkart.connekt.receptors.routes.Stencils

import java.util.Date

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.{AppUser, Stencil, Bucket}
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
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
                        complete(respond[GenericResponse](
                          StatusCodes.Created, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.Created.intValue, null, Response("Bucket create for name %s".format(bucket.name), Map("id" -> id)))
                        ))
                      case Failure(e) =>
                        complete(respond[GenericResponse](
                          StatusCodes.BadRequest, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Bucket cannot be created for name %s".format(bucket.name), e))
                        ))
                    }
                  }
                } ~ get {
                  authorize(user, "ADMIN_BUCKET") {
                    StencilService.getBucket(name) match {
                      case Some(bucket) =>
                        complete(respond[GenericResponse](
                              StatusCodes.OK, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.OK.intValue, null, Response("Bucket found for name %s".format(bucket.name), Map("bucket" -> bucket)))
                            ))
                      case _ =>
                        complete(respond[GenericResponse](
                              StatusCodes.BadRequest, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Bucket not found for name %s".format(name), null))
                            ))
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
                            val channelRequestData = StencilService.render(stencil, entity)
                            complete(respond[GenericResponse](
                              StatusCodes.OK, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.OK.intValue, null, Response("Stencils fetched for id: %s".format(id), Map[String, Any]("channelRequest" -> channelRequestData)))
                            ))
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
                                    val channelRequestData = StencilService.render(stnc, entity)
                                    complete(respond[GenericResponse](
                                      StatusCodes.OK, Seq.empty[HttpHeader],
                                      GenericResponse(StatusCodes.OK.intValue, null, Response("Stencils fetched for id: %s".format(id), Map[String, Any]("channelRequest" -> channelRequestData)))
                                    ))

                                  case None =>
                                    complete(respond[GenericResponse](
                                      StatusCodes.BadRequest, Seq.empty[HttpHeader],
                                      GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Stencils Not found for id: %s".format(id), null))
                                    ))
                                }
                              }
                            }
                          }
                        } ~ pathEndOrSingleSlash {
                          get {
                            authorize(user, bucketIds.map("STENCIL_GET_" + _): _*) {
                              StencilService.get(id, Option(version)) match {
                                case Some(stnc) =>
                                  complete(respond[GenericResponse](
                                  StatusCodes.OK, Seq.empty[HttpHeader],
                                  GenericResponse(StatusCodes.OK.intValue, null, Response("Stencils fetched for id: %s".format(id), Map[String, Any]("stencils" -> stencil)))
                                ))
                                case None =>
                                  complete(respond[GenericResponse](
                                  StatusCodes.BadRequest, Seq.empty[HttpHeader],
                                  GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Stencil not found for name %s with version: %s".format(id, version), null))
                                ))
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
                                complete(respond[GenericResponse](
                                 StatusCodes.OK, Seq.empty[HttpHeader],
                                 GenericResponse(StatusCodes.OK.intValue, null, Response("Stencil registered for %s".format(id), null))))
                              case _ =>
                                complete(respond[GenericResponse](
                                 StatusCodes.BadRequest, Seq.empty[HttpHeader],
                                 GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Error in Stencil for %s".format(stencil.id), null))))
                            }
                          }
                        }
                      } ~ get {
                        authorize(user, bucketIds.map("STENCIL_GET_" + _): _*) {
                          complete(respond[GenericResponse](
                            StatusCodes.OK, Seq.empty[HttpHeader],
                            GenericResponse(StatusCodes.OK.intValue, null, Response("Stencils fetched for id: %s".format(id), Map[String, Any]("stencils" -> stencil)))
                          ))
                        }
                      }
                    }
                  case None =>
                    complete(respond[GenericResponse](
                              StatusCodes.BadRequest, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Stencil not found for name %s".format(id), null))
                            ))

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

                    StencilService.add(stencil) match {
                      case Success(sten) =>
                        complete(respond[GenericResponse](
                        StatusCodes.Created, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.Created.intValue, null, Response("Stencil registered with %s".format(stencil.id), Map("id" -> stencil.id)))))
                      case Failure(e) =>
                        complete(respond[GenericResponse](
                        StatusCodes.BadRequest, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Error in Stencil for %s".format(stencil.id), e))))
                    }
                  }
                }
              }
            }
          }
    }
}
