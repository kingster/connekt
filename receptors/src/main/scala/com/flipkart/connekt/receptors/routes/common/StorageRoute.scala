package com.flipkart.connekt.receptors.routes.common

import _root_.akka.stream.ActorMaterializer
import akka.http.scaladsl.model.StatusCodes
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.receptors.directives.FileDirective
import com.flipkart.connekt.receptors.routes.BaseHandler
import com.flipkart.connekt.commons.utils.StringUtils._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by nidhi.mehla on 18/02/16.
 */
class StorageRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler with FileDirective {

  //implicit val umS = PredefinedFromEntityUnmarshallers.stringUnmarshaller

  val route =
    pathPrefix("v1" / "storage") {

      path(Segment) {
        (key: String) =>
          put {
            uploadFile { fileMap =>
              implicit ctx =>
                fileMap.isEmpty match {
                  case true =>
                    val stringData: String = ctx.request.entity.getString
                    ServiceFactory.getStorageService.put(key, stringData).get
                  case false =>
                    val fileBytes = scala.io.Source.fromFile(fileMap.head._2.targetFile).map(_.toByte).toArray
                    ServiceFactory.getStorageService.put(key, fileBytes).get
                }

                ctx.complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Succesfully stored.", Map())))
            }
          } ~ get {
            sniffHeaders { headers =>

              val data = ServiceFactory.getStorageService.get(key).get
              val requestType = headers.find(_.is("Accept")).map(_.value())

              requestType match {
                case Some(x) if x.equalsIgnoreCase("text/plain(UTF-8)") =>
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Succesfully stored.", Map("value" -> new String(x)))))
                case _ =>
                  complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Succesfully stored.", Map())))
              }
            }
          }
      }

    }
}