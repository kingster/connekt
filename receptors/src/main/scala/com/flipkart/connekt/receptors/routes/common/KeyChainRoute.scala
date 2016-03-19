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
package com.flipkart.connekt.receptors.routes.common

import java.io.File

import _root_.akka.stream.ActorMaterializer
import akka.http.scaladsl.model.StatusCodes
import com.flipkart.connekt.commons.entities.MobilePlatform.MobilePlatform
import com.flipkart.connekt.commons.entities.MobilePlatform._
import com.flipkart.connekt.commons.entities._
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.receptors.directives.{FileDirective, MPlatformSegment}
import com.flipkart.connekt.receptors.routes.BaseHandler

import scala.util.{Failure, Success}

class KeyChainRoute(implicit am: ActorMaterializer, user: AppUser) extends BaseHandler with FileDirective {

  /**
   * formFields doesn't work now.
   *
   * Issue : https://github.com/akka/akka/issues/18591
   * Issue : https://github.com/akka/akka/issues/19506
   *
   * When the issues get resolved,  extractFormData for simple fields can be removed, and moved to formFields, which also adds in validation.
   */

  val route =
    pathPrefix("v1" / "keychain") {
      authorize(user, "ADMIN_KEYCHAIN") {
        path(Segment / MPlatformSegment) {
          (appName: String, platform: MobilePlatform) =>
            platform match {
              case IOS =>
                post {
                  extractFormData { postMap =>

                    val fileInfo = postMap("file").right.get
                    val password = postMap("password").left.get

                    fileInfo.status match {

                      case Success(x) =>
                        val credential = AppleCredential(new File(fileInfo.tmpFilePath), password)
                        KeyChainManager.addAppleCredentials(appName, credential)
                        complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $appName", credential)))

                      case Failure(e) =>
                        //There was some isse processing the fileupload.
                        ConnektLogger(LogFile.SERVICE).error("Credentials Upload File Error", e)
                        complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("There was some error processing your request", Map("debug" -> e.getMessage))))
                    }

                  }
                } ~ get {
                  KeyChainManager.getAppleCredentials(appName) match {
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
                    KeyChainManager.addGoogleCredential(appName, credential)
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $appName", credential)))
                  }
                } ~ get {
                  KeyChainManager.getGoogleCredential(appName) match {
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
                    KeyChainManager.addMicrosoftCredential(appName, credential)
                    complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $platform / $appName", credential)))
                  }
                } ~ get {
                  KeyChainManager.getMicrosoftCredential(appName) match {
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

        } ~ path(Segment) {
          key: String =>
            post {
              extractFormData { postMap =>
                val username: String = postMap("username").left.get
                val password: String = postMap("password").left.get
                val credential = SimpleCredential(username, password)
                KeyChainManager.addSimpleCredential(key, credential)
                complete(GenericResponse(StatusCodes.OK.intValue, null, Response(s"Succesfully stored $key", credential)))
              }
            } ~ get {
              KeyChainManager.getSimpleCredential(key) match {
                case Some(x) =>
                  complete(GenericResponse(StatusCodes.OK.intValue, null, Response("Credentials Found.", x)))
                case None =>
                  complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Not Found.", null)))
              }
            }

        }
      }

    }
}
