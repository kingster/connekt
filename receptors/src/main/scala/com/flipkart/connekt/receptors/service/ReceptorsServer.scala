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
package com.flipkart.connekt.receptors.service

import java.security.{KeyStore, SecureRandom}
import java.util.UUID
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.iomodels.{GenericResponse, Response}
import com.flipkart.connekt.commons.services.ConnektConfig
import com.flipkart.connekt.receptors.directives.{AccessLogDirective, CORSDirectives}
import com.flipkart.connekt.receptors.routes.{BaseJsonHandler, RouteRegistry}
import com.flipkart.connekt.receptors.wire.ResponseUtils._
import scala.collection.immutable
import scala.concurrent.Future
object ReceptorsServer extends BaseJsonHandler with AccessLogDirective with CORSDirectives {

  implicit val system = ActorSystem("ckt-receptors")
  implicit val mat = ActorMaterializer.create(system)
  implicit val ec = system.dispatcher

  private val bindHost = ConnektConfig.getString("receptors.bindHost").getOrElse("0.0.0.0")
  private val bindPort = ConnektConfig.getInt("receptors.bindPort").getOrElse(28000)

  private val enableHttps = ConnektConfig.getBoolean("receptors.https.enabled").getOrElse(false)
  private val bindHttpsPort = ConnektConfig.getInt("receptors.https.port").getOrElse(28443)

  var httpService: Future[ServerBinding] = null
  var httpsService: Future[ServerBinding] = null

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle{
      case AuthenticationFailedRejection(cause, _ ) =>  logTimedRequestResult {
        complete(GenericResponse(StatusCodes.Unauthorized.intValue, null, Response("Authentication Failed, Please Contact connekt-dev@flipkart.com", null)))
      }
      case AuthorizationFailedRejection => logTimedRequestResult {
        complete(GenericResponse(StatusCodes.Forbidden.intValue, null, Response("UnAuthorised Access, Please Contact connekt-dev@flipkart.com", null)))
      }
      case MalformedRequestContentRejection(msg, _) => logTimedRequestResult {
        complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Malformed Content, Unable to Process Request", Map("debug" -> msg))))
      }
      case TokenAuthenticationFailedRejection(msg) => logTimedRequestResult {
        complete(GenericResponse(StatusCodes.Forbidden.intValue, null, Response("Secure Code Validation Failed.", msg)))
      }
    }.handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete(GenericResponse(StatusCodes.MethodNotAllowed.intValue, null, Response(s"Can't do that! Supported: ${names mkString " or "}!", null)))
    }.handleNotFound {
      logTimedRequestResult {
        complete(GenericResponse(StatusCodes.NotFound.intValue, null, Response("Oh man, what you are looking for is long gone.", null)))
      }
    }.result()

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case rejection: IllegalArgumentException => logTimedRequestResult {
      complete(GenericResponse(StatusCodes.BadRequest.intValue, null, Response("Malformed Content, Unable to Process Request", Map("debug" -> rejection.getMessage))))
    }
    case e: Throwable =>
      val errorUID: String = UUID.randomUUID.getLeastSignificantBits.abs.toString
      ConnektLogger(LogFile.SERVICE).error(s"API ERROR # -- $errorUID  --  Reason [ ${e.getMessage} ]", e)
      val response = Map("message" -> ("Server Error # " + errorUID), "reason" -> e.getMessage)
      logTimedRequestResult {
        complete(GenericResponse(StatusCodes.InternalServerError.intValue, null, Response("Internal Server Error", response)))
      }
  }

  val allRoutes = cors {
    new RouteRegistry().allRoutes
  }

  def routeWithLogging = ConnektConfig.getString("http.request.log").getOrElse("true").toBoolean match {
    case true => logTimedRequestResult(allRoutes)
    case false => allRoutes
  }

  def apply() = {
    httpService = Http().bindAndHandle(routeWithLogging, bindHost, bindPort)

    if(enableHttps) {

      ConnektLogger(LogFile.SERVICE).info("Receptor Server HTTPS enabled... enabling https port")

      val httpsContext = {
        val keystorePassword = "changeit".toCharArray
        val context = SSLContext.getInstance("TLS")

        val keyStore = KeyStore.getInstance("jks")
        keyStore.load(getClass.getClassLoader.getResourceAsStream("nm.flipkart.jks"), keystorePassword)

        val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        keyManagerFactory.init(keyStore, keystorePassword)

        val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
        trustManagerFactory.init(keyStore)

        context.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

        //Best ciphers based on http://security.stackexchange.com/questions/76993/now-that-it-is-2015-what-ssl-tls-cipher-suites-should-be-used-in-a-high-securit
        val allowedCiphers  = context.getDefaultSSLParameters.getCipherSuites.toSeq
          .intersect(Seq("TLS_DHE_RSA_WITH_AES_256_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384","TLS_DHE_RSA_WITH_AES_256_GCM_SHA384","TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"))

        ConnectionContext.https(
          sslContext =  context,
          enabledProtocols = Option(immutable.Seq("TLSv1"))
          //enabledCipherSuites = Option(immutable.Seq[String](allowedCiphers: _*)) //For enhanced security
        )
      }

      httpsService = Http().bindAndHandle(routeWithLogging, bindHost, bindHttpsPort, httpsContext)
    }
  }

  def shutdown() = {
    val httpShutdown = httpService.flatMap(_.unbind())
    val httpsShutdown = Option(httpsService).map(_.flatMap(_.unbind())).getOrElse(FastFuture.successful(Unit))

    Future.sequence(List(httpShutdown,httpsShutdown)).onComplete(_ => {
        ConnektLogger(LogFile.SERVICE).info("receptor server unbinding complete")

      /**
       * Shutdown  will sometimes throw error message like
       *
       * 2016-05-12 01:48:34,514 ERROR ActorSystemImpl [ckt-receptors-akka.actor.default-dispatcher-6] Outgoing response stream error
       * akka.stream.AbruptTerminationException: Processor actor [Actor[akka://ckt-receptors/user/StreamSupervisor-0/flow-2-0-unknown-operation#-100715831]] terminated abruptly
       * [ERROR] [05/12/2016 01:48:34.501] [ckt-receptors-akka.actor.default-dispatcher-11] [akka.actor.ActorSystemImpl(ckt-receptors)] Outgoing response stream error (akka.stream.AbruptTerminationException)
       *
       * But this can be ignored. Ref : https://github.com/akka/akka/issues/18747
       */
        system.terminate()
      })
  }

}
