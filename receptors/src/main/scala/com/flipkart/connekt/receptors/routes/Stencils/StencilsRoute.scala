package com.flipkart.connekt.receptors.routes.Stencils

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.entities.Stencil
import com.flipkart.connekt.commons.iomodels.{ConnektRequest, GenericResponse, Response}
import com.flipkart.connekt.commons.services.StencilService
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.collection.immutable.Seq

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
                    StencilService.add(stencil)
                    complete(respond[GenericResponse](
                    StatusCodes.Created, Seq.empty[HttpHeader],
                    GenericResponse(StatusCodes.Created.intValue, null, Response("Stencil registered for %s".format(stencil.id), null))))
                  }
                }
              }
            } ~ path(Segment) {
              (id: String) =>
                put {
                  authorize(user, "STENCIL_UPDATE") {
                    entity(as[Stencil]) { stencil =>
                      stencil.id = id
                      StencilService.update(stencil)
                      complete(respond[GenericResponse](
                        StatusCodes.OK, Seq.empty[HttpHeader],
                        GenericResponse(StatusCodes.OK.intValue, null, Response("Stencil updated for %s".format(stencil.id), null))))

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
                  authorize(user, "STENCIL_PREVIEW") {
                    entity(as[ConnektRequest]) { request =>
                      val req = request.copy(templateId = id)
                      StencilService.render(req) match {
                        case channelRequest =>
                          complete(respond[GenericResponse](
                                StatusCodes.OK, Seq.empty[HttpHeader],
                                GenericResponse(StatusCodes.OK.intValue, null, Response("Preview fetched for id: %s".format(id), Map[String, Any]("channelRequest" -> channelRequest)))
                              ))
                        case _ =>
                          complete(respond[GenericResponse](
                            StatusCodes.BadRequest, Seq.empty[HttpHeader],
                            GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Fetching Stencils failed for id:%s ".format(id), null))
                          ))

                      }
                    }
                  }
                }
            }
          }
      }
    }

}
