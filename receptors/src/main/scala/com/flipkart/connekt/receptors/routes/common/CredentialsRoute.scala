package com.flipkart.connekt.receptors.routes.common

import java.io.File

import _root_.akka.stream.ActorMaterializer
import akka.http.scaladsl.model.StatusCodes
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.entities.{AppUser, AppleCredential, GoogleCredential, MicrosoftCredential}
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.CredentialManager
import com.flipkart.connekt.receptors.directives.{FileDirective, MPlatformSegment}
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.util.{Failure, Success}

/**
 * Created by nidhi.mehla on 18/02/16.
 */
class CredentialsRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler with FileDirective {

  /**
   * formFields doesn't work now.
   *
   * Issue : https://github.com/akka/akka/issues/18591
   * Issue : https://github.com/akka/akka/issues/19506
   *
   * When the issues get resolved,  extractFormData for simple fields can be removed, and moved to formFields, which also adds in validation.
   */

  val route =
    pathPrefix("v1" / "credentials") {

      path(Segment / MPlatformSegment) {
        (appName: String, platform: MobilePlatform) =>
          val storageKey = s"$platform.$appName"
          platform match {
            case IOS =>
              post {
                extractFormData { postMap =>

                  val fileInfo = postMap("file").right.get
                  val password = postMap("password").left.get

                  fileInfo.status match {

                    case Success(x) =>
                      val credential = AppleCredential(new File(fileInfo.tmpFilePath), password)
                      CredentialManager.addAppleCredentials(storageKey,credential )
                      complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $appName", credential)))

                    case Failure(e) =>
                      //There was some isse processing the fileupload.
                      ConnektLogger(LogFile.SERVICE).error("Credentials Upload File Error", e)
                      complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("There was some error processing your request", Map("debug" -> e.getMessage))))
                  }

                }
              } ~ get {
                CredentialManager.getAppleCredentials(storageKey) match {
                  case Some(x) =>
                    //TODO : Serve the file here.
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Credentials Found.", x)))
                  case None =>
                    complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Not Found.", null)))
                }
              }

            case ANDROID =>
              post {
                //TODO : Fix when the issue is resolved.
                //formFields('appId, 'appKey) { (appId, appKey) =>
                extractFormData { postMap =>
                  val appId: String = postMap("appId").left.get
                  val appKey: String = postMap("appKey").left.get

                  val credential = GoogleCredential(appId, appKey)
                  CredentialManager.addGoogleCredential(storageKey, credential)
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $appName", credential)))
                }
              } ~ get {
                CredentialManager.getGoogleCredential(storageKey) match {
                  case Some(x) =>
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Credentials Found.", x)))
                  case None =>
                    complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Not Found.", null)))
                }
              }

            case WINDOWS =>
              post {
                //TODO : Fix when the issue is resolved.
                //formFields('clientId, 'secret) { (clientId, clientSecret) =>
                extractFormData { postMap =>
                  val clientId: String = postMap("clientId").left.get
                  val clientSecret: String = postMap("secret").left.get
                  val credential = MicrosoftCredential(clientId, clientSecret)
                  CredentialManager.addMicrosoftCredential(storageKey,credential )
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $appName", credential)))
                }
              } ~ get {
                CredentialManager.getMicrosoftCredential(storageKey) match {
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