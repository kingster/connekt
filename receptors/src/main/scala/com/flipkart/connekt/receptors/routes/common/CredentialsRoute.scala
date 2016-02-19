package com.flipkart.connekt.receptors.routes.common

import _root_.akka.stream.ActorMaterializer
import akka.http.scaladsl.model.StatusCodes
import com.flipkart.connekt.commons.entities.{MicrosoftCredential, GoogleCredential, AppUser}
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.factories.ServiceFactory
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.CredentialManager
import com.flipkart.connekt.receptors.directives.{MPlatformSegment, FileDirective}
import com.flipkart.connekt.receptors.routes.BaseHandler
import com.flipkart.connekt.commons.utils.StringUtils._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by nidhi.mehla on 18/02/16.
 */
class CredentialsRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler with FileDirective {

  val route =
    pathPrefix("v1" / "storage" / "credentials") {

      path(Segment / MPlatformSegment) {
        (key: String, platform: MobilePlatform) =>

          platform match {
            case IOS =>
              post {
                uploadFile {
                  fileMap =>
                    parameters('password ) { (password) =>

                      println(password)
                      println(fileMap)

                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $key", null)))
                    }
                }
              } ~ get {
                CredentialManager.getAppleCredentials(key) match {
                  case Some(x) =>
                    //TODO : Serve the file here.
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Credentials Found.", x)))
                  case None =>
                    complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Not Found.", null)))
                }
              }

            case ANDROID =>
              post {
                parameters('appId, 'appKey) { (appId, appKey) =>
                  CredentialManager.addGoogleCredential(key, GoogleCredential(appId, appKey))
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $key", null)))
                }
              } ~ get {
                CredentialManager.getGoogleCredential(key) match {
                  case Some(x) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Credentials Found.", x)))
                  case None =>
                    complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Not Found.", null)))
                }
              }

            case WINDOWS =>
              post {
                parameters('clientId, 'secret) { (clientId, clientSecret) =>
                  CredentialManager.addMicrosoftCredential(key, MicrosoftCredential(clientId, clientSecret))
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $key", null)))
                }
              } ~ get {

                CredentialManager.getMicrosoftCredential(key) match {
                  case Some(x) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Credentials Found.", x)))
                  case None =>
                    complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Not Found.", null)))
                }
              }


            case _ =>
              post {
                complete(GenericResponse(StatusCodes.NotImplemented.intValue, null, Response("Not Supported.", null)))
              } ~ get {
                complete(GenericResponse(StatusCodes.NotImplemented.intValue, null, Response("Not Supported.", null)))
              }

          }

      }


    }
}