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
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.flipkart.connekt.commons.entities.{Bucket, Stencil, StencilsEnsemble}
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels._
import com.flipkart.connekt.commons.sync.{SyncManager, SyncMessage, SyncType}
import com.flipkart.connekt.commons.utils.StringUtils
import com.flipkart.connekt.commons.utils.StringUtils._
import com.flipkart.connekt.receptors.routes.BaseJsonHandler
import com.flipkart.connekt.receptors.wire.ResponseUtils._

import scala.util.{Failure, Success}

class StencilsRoute(implicit am: ActorMaterializer) extends BaseJsonHandler {

  lazy val stencilService = ServiceFactory.getStencilService

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
                      stencilService.addBucket(bucket) match {
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
                      stencilService.getBucket(name) match {
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
            } ~ pathPrefix("components" / "registry") {
              pathEndOrSingleSlash {
                post {
                  meteredResource("addStencilComponents") {
                    authorize(user, "ADMIN_BUCKET") {
                      entity(as[StencilsEnsemble]) { obj =>
                        val id = StringUtils.generateRandomStr(4)
                        val stencilComponents = new StencilsEnsemble()
                        stencilComponents.id = id
                        stencilComponents.components = obj.components.toLowerCase.replaceAll("\\s", "")
                        stencilComponents.name = obj.name.toUpperCase.trim
                        stencilComponents.createdBy = user.userId
                        stencilComponents.updatedBy = user.userId
                        stencilService.getStencilsEnsembleByName(stencilComponents.name) match {
                          case Some(s) => complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil components with `name` ${stencilComponents.name} already exists.", null)))
                          case None =>
                            stencilService.addStencilComponents(stencilComponents) match {
                              case Success(sten) =>
                                complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Stencil components registered with id: $id", Map("StencilComponents" -> stencilComponents))))
                              case Failure(e) =>
                                complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil components for id: $id, e: ${e.getMessage}", null)))
                            }
                        }
                      }
                    }
                  }
                } ~ get {
                  meteredResource("getAllStencilComponents") {
                    authorize(user, "ADMIN_BUCKET") {
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"All Ensembles fetched", stencilService.getAllEnsemble().map(ensemble => ensemble.name -> ensemble.components.split(",")).toMap)))
                    }
                  }
                }
              } ~ path(Segment) {
                (id: String) =>
                  put {
                    meteredResource("stencilComponentsUpdate") {
                      authorize(user, "ADMIN_BUCKET") {
                        entity(as[StencilsEnsemble]) { obj =>
                          stencilService.getStencilsEnsemble(id) match {
                            case Some(stencilComponents) =>
                              val stencilComponents = new StencilsEnsemble()
                              stencilComponents.id = id
                              stencilComponents.components = obj.components.toLowerCase.replaceAll("\\s", "")
                              stencilComponents.name = obj.name.toUpperCase.trim
                              stencilComponents.updatedBy = user.userId
                              stencilComponents.creationTS = stencilComponents.creationTS
                              stencilService.addStencilComponents(stencilComponents) match {
                                case Success(sten) =>
                                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencil components updated with id: $id", Map("stencilComponents" -> stencilComponents))))
                                case Failure(e) =>
                                  complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil components for id: $id, e: ${e.getMessage}", null)))
                              }
                            case None =>
                              complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil components not found for id: $id", null)))
                          }
                        }
                      }
                    }
                  } ~ get {
                    meteredResource("getStencilComponents") {
                      authorize(user, "ADMIN_BUCKET") {
                        stencilService.getStencilsEnsemble(id) match {
                          case Some(stencilComponents) =>
                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencil components fetched for id: $id", Map("stencilComponents" -> stencilComponents))))
                          case None =>
                            complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil components not found for id: $id", null)))
                        }
                      }
                    }
                  }
              } ~ path(Segment / "touch") {
                (id: String) =>
                  post {
                    SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_COMPONENTS_UPDATE, List(id)))
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Triggered  Change for stencil components: $id", null)))
                  }
              }
            } ~ pathPrefix(Segment) {
              (id: String) =>
                path("preview") {
                  post {
                    meteredResource("stencilPreviewHead") {
                      entity(as[ObjectNode]) { entity =>
                        stencilService.get(id) match {
                          case stencils if stencils.nonEmpty =>
                            val bucketIds = stencils.head.bucket.split(",")
                            authorize(user, bucketIds.map("STENCIL_PREVIEW_" + _): _*) {
                              val preview = stencils.map(stencil => {
                                stencil.component -> stencilService.materialize(stencil, entity)
                              }).toMap
                              complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", preview)))
                            }
                          case _ =>
                            complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil not found for name: $id", null)))
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
                            stencilService.get(id, Some(version)) match {
                              case stencils if stencils.nonEmpty =>
                                authorize(user, stencils.head.bucket.split(",").map("STENCIL_PREVIEW_" + _): _*) {
                                  val preview = stencils.map(stencil => {
                                    stencil.component -> stencilService.materialize(stencil, entity)
                                  }).toMap
                                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", preview)))
                                }
                              case _ =>
                                complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencils Not found for id: $id", null)))
                            }
                          }
                        }

                      }
                    } ~ path(Segment / "touch") {
                      (component: String) =>
                        post {
                          meteredResource("stencilTouch") {
                            SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_CHANGE, List(id, version)))
                            SyncManager.get().publish(new SyncMessage(SyncType.STENCIL_FABRIC_CHANGE, List(stencilService.fabricCacheKey(id, component, version))))
                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Triggered  Change for client: $id", null)))
                          }
                        }
                    } ~ pathEndOrSingleSlash {
                      get {
                        meteredResource("stencilGetVersion") {
                          stencilService.get(id, Option(version)) match {
                            case stnc if stnc.nonEmpty =>
                              authorize(user, stnc.head.bucket.split(",").map("STENCIL_GET_" + _): _*) {
                                complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("stencils" -> stnc))))
                              }
                            case _ =>
                              complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil not found for name: $id with version: $version", null)))
                          }
                        }
                      }
                    }
                } ~ pathEndOrSingleSlash {
                  put {
                    meteredResource("stencilUpdate") {
                      stencilService.get(id) match {
                        case stencils if stencils.nonEmpty =>
                          authorize(user, stencils.head.bucket.split(",").map("STENCIL_UPDATE_" + _): _*) {
                            entity(as[ObjectNode]) { obj =>
                              val stencilName = obj.get("name").asText()
                              val components = obj.get("components").asInstanceOf[ArrayNode].elements()
                              val bucket = obj.get("bucket").asText()
                              val bucketIds = bucket.split(",").map(stencilService.getBucket(_).map(_.id.toUpperCase).getOrElse("")).filter(_ != "")
                              var stencilsUpdate = List[Stencil]()
                              try {
                                while (components.hasNext) {
                                  val c = components.next()
                                  var stencil = c.toString.getObj[Stencil]
                                  stencil.bucket = bucketIds.mkString(",")
                                  stencil.id = id
                                  stencil.createdBy = stencils.head.createdBy
                                  stencil.updatedBy = user.userId
                                  stencil.version = stencil.version + 1
                                  stencil.name = stencilName
                                  stencil.creationTS = stencils.head.creationTS
                                  stencil.lastUpdatedTS = new Date(System.currentTimeMillis())
                                  stencilService.checkStencil(stencil) match {
                                    case Success(_) => stencilsUpdate ::= stencil
                                    case Failure(e) => throw e
                                  }
                                }
                                // If stencil name is changed, deleting old stencil and creating new
                                if (stencils.head.name.equals(stencilName)) {
                                  stencilService.update(id, stencilsUpdate) match {
                                    case Success(sten) =>
                                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencil updated for id: $id", null)))
                                    case _ =>
                                      complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil for id: $id", null)))
                                  }
                                } else {
                                  stencilService.updateWithIdentity(id, stencils.head.name, stencilsUpdate) match {
                                    case Success(sten) =>
                                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencil updated for id: $id", null)))
                                    case _ =>
                                      complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil for id: $id", null)))
                                  }
                                }
                              } catch {
                                case e: Throwable =>
                                  complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Error in Stencil for id: $id, e: ${e.getMessage}", null)))
                              }
                            }
                          }
                        case _ =>
                          complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil not found for id: $id", null)))
                      }
                    }
                  } ~ get {
                    meteredResource("stencilGet") {
                      stencilService.get(id) match {
                        case stencils if stencils.nonEmpty =>
                          authorize(user, stencils.head.bucket.split(",").map("STENCIL_GET_" + _): _*) {
                            complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Stencils fetched for id: $id", Map[String, Any]("stencils" -> stencils))))
                          }
                        case _ =>
                          complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response(s"Stencil not found for id: $id", null)))
                      }
                    }
                  }
                }
            } ~ pathEndOrSingleSlash {
              post {
                meteredResource("stencilAdd") {
                  entity(as[ObjectNode]) { obj =>
                    var stencils = List[Stencil]()
                    val stencilName = obj.get("name").asText()
                    val stencilId = "STNC" + StringUtils.generateRandomStr(4)
                    val components = obj.get("components").asInstanceOf[ArrayNode].elements()
                    val bucket = obj.get("bucket").asText()
                    val bucketIds = bucket.split(",").map(stencilService.getBucket(_).map(_.id.toUpperCase).getOrElse("")).filter(_ != "")
                    val resources = bucketIds.map("STENCIL_UPDATE_" + _)
                    authorize(user, resources: _*) {
                      try {
                        while (components.hasNext) {
                          val stencil = components.next().toString.getObj[Stencil]
                          stencil.bucket = bucketIds.mkString(",")
                          stencil.id = stencilId
                          stencil.createdBy = user.userId
                          stencil.updatedBy = user.userId
                          stencil.version = 1
                          stencil.name = stencilName
                          stencil.creationTS = new Date(System.currentTimeMillis())
                          stencil.lastUpdatedTS = new Date(System.currentTimeMillis())
                          stencilService.checkStencil(stencil)
                          stencils ::= stencil
                        }

                        stencilService.add(stencilId, stencils) match {
                          case Success(sten) =>
                            complete(GenericResponse(StatusCodes.Created.intValue, null, Response(s"Stencil registered with id: $stencilId", Map("id" -> stencilId))))
                          case Failure(e) =>
                            complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Error in Stencil.", e.getMessage)))
                        }
                      } catch {
                        case e: Throwable =>
                          complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Error in Stencil.", e.getMessage)))
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
