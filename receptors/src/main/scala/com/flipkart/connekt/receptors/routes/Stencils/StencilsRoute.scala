package com.flipkart.connekt.receptors.routes.Stencils

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.connekt.commons.entities.Stencil
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

/**
 * @author aman.shrivastava on 19/01/16.
 */
class StencilsRoute(implicit am: ActorMaterializer) extends BaseHandler {
  val stencils =
    pathPrefix("v1") {
      authenticate {
        user =>
          pathPrefix("stencils") {
            pathEndOrSingleSlash {
              post {
                authorize(user, "STENCILS_CREATE") {
                  entity(as[Stencil]) { stencil =>
                    stencil.id = "STNC" + StringUtils.generateRandomStr(4)
                    stencil.createdBy = user.userId
                    stencil.updatedBy = user.userId
                    stencil.version = 1
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
            } ~ path(Segment) {
              (id: String) =>
                put {
                  authorize(user, "STENCIL_UPDATE" + user.groups) {
                    entity(as[Stencil]) { stencil =>
                      StencilService.get(id) match {
                        case Some(sten) =>
                          stencil.id = id
                          stencil.updatedBy = user.userId
                          stencil.version += 1
                          StencilService.add(stencil) match {
                            case Success(sten) =>
                              complete(respond[GenericResponse](
                               StatusCodes.OK, Seq.empty[HttpHeader],
                               GenericResponse(StatusCodes.OK.intValue, null, Response("Stencil registered for %s".format(stencil.id), null))))
                            case _ =>
                              complete(respond[GenericResponse](
                               StatusCodes.BadRequest, Seq.empty[HttpHeader],
                               GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Error in Stencil for %s".format(stencil.id), null))))
                          }
                        case _ =>
                          complete(respond[GenericResponse](
                          StatusCodes.BadRequest, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Stencil Not found for %s".format(stencil.id), null))))
                      }

                    }
                  }
                } ~
                  get {
                    StencilService.get(id) match {
                      case stencil =>
                        complete(respond[GenericResponse](
                                StatusCodes.OK, Seq.empty[HttpHeader],
                                GenericResponse(StatusCodes.OK.intValue, null, Response("Stencils fetched for id: %s".format(id), Map[String, Any]("stencils" -> stencil)))
                              ))
                      case _ =>
                        complete(respond[GenericResponse](
                            StatusCodes.BadRequest, Seq.empty[HttpHeader],
                            GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Fetching Stencils failed for id:%s ".format(id), null))
                          ))
                    }


                  }
            } ~ path(Segment / "preview") {
              (id: String) =>
                post {
                  entity(as[ObjectNode]) { entity =>
                    StencilService.get(id) match {
                      case stencil =>
                        StencilService.render(stencil, entity) match {
                          case Some(channelRequest) =>
                            complete(respond[GenericResponse](
                              StatusCodes.OK, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.OK.intValue, null, Response("Stencils fetched for id: %s".format(id), Map[String, Any]("channelRequest" -> channelRequest)))
                          ))
                          case None =>
                            complete(respond[GenericResponse](
                              StatusCodes.BadRequest, Seq.empty[HttpHeader],
                              GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Stencils cannot be render for id: %s".format(id), null))
                            ))
                        }
                      case None =>
                        complete(respond[GenericResponse](
                          StatusCodes.BadRequest, Seq.empty[HttpHeader],
                          GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Stencils Not found for id: %s".format(id), null))
                        ))

                    }
                  }
                }
            }
          }
      }
    }
}
