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
package com.flipkart.connekt.busybees.streams.flows.dispatchers

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import akka.http.scaladsl.util.FastFuture
import akka.stream.scaladsl.Flow
import com.flipkart.connekt.busybees.models.APNSRequestTracker
import com.flipkart.connekt.commons.factories.{ConnektLogger, LogFile}
import com.flipkart.connekt.commons.services.KeyChainManager
import com.flipkart.connekt.commons.utils.FutureUtils._
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification
import com.relayrides.pushy.apns.{ApnsClient, ClientNotConnectedException, PushNotificationResponse}
import io.netty.channel.nio.NioEventLoopGroup

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.Try
import scala.util.control.NonFatal

object APNSDispatcher {

  private [busybees] val clientGatewayCache = new ConcurrentHashMap[String, Future[ApnsClient[SimpleApnsPushNotification]]]

  private def createAPNSClient(appName: String): ApnsClient[SimpleApnsPushNotification] = {
    ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher starting $appName apns-client")
    val credential = KeyChainManager.getAppleCredentials(appName).get
    //TODO: shutdown this eventloop when client is closed.
    val eventLoop = new NioEventLoopGroup(4, new ThreadFactoryBuilder().setNameFormat(s"apns-nio-$appName").build())
    val client = new ApnsClient[SimpleApnsPushNotification](credential.getCertificateFile, credential.passkey,eventLoop)
    client.connect(ApnsClient.PRODUCTION_APNS_HOST).await(120, TimeUnit.SECONDS)
    if (!client.isConnected)
      ConnektLogger(LogFile.PROCESSORS).error(s"APNSDispatcher Unable to connect [$appName] apns-client in 2minutes")
    client
  }

  private [busybees]  def cachedGateway(appName: String): Future[ApnsClient[SimpleApnsPushNotification]] = {
    val gatewayPromise = Promise[ApnsClient[SimpleApnsPushNotification]]()
    clientGatewayCache.putIfAbsent(appName, gatewayPromise.future) match {
      case null ⇒ // only one thread can get here at a time
        val gateway =
        //TODO: Move to a pooled client(as recommended by apns)
          try createAPNSClient(appName)
          catch {
            case NonFatal(e) =>
              clientGatewayCache.remove(appName)
              gatewayPromise.failure(e)
              throw e
          }
        val fastFuture = FastFuture.successful(gateway)
        clientGatewayCache.put(appName, fastFuture) // optimize subsequent gateway accesses
        gatewayPromise.success(gateway) // satisfy everyone who got a hold of our promise while we were starting up
        fastFuture
      case future ⇒ future // return cached instance
    }
  }

  sys.addShutdownHook {
    // Disconnect all the APNSClients prior to stop
    clientGatewayCache.asScala.foreach(kv => {
      ConnektLogger(LogFile.PROCESSORS).info(s"APNSDispatcher stopping ${kv._1} apns-client")
      kv._2.map(_.disconnect().awaitUninterruptibly(5000))(scala.concurrent.ExecutionContext.Implicits.global)
    })
  }


}
class APNSDispatcher(parallelism: Int)(implicit ec: ExecutionContextExecutor) {

  import APNSDispatcher._

  def flow = {

    Flow[(SimpleApnsPushNotification, APNSRequestTracker)].mapAsyncUnordered(parallelism) {
      case (request, userContext) ⇒
        val result = Promise[(Try[PushNotificationResponse[SimpleApnsPushNotification]], APNSRequestTracker)]()

        val gatewayFuture = cachedGateway(userContext.appName)

        gatewayFuture
          .flatMap(client => client.sendNotification(request).asScala.recoverWith {
              case nce: ClientNotConnectedException =>
                ConnektLogger(LogFile.PROCESSORS).debug("APNSDispatcher waiting for apns-client to reconnect")
                client.getReconnectionFuture.awaitUninterruptibly()
                ConnektLogger(LogFile.PROCESSORS).debug(s"APNSDispatcher apns-client reconnected with status [${client.isConnected}]")
                if(!client.isConnected){
                  ConnektLogger(LogFile.PROCESSORS).debug(s"APNSDispatcher apns-client reconnect error", client.getReconnectionFuture.cause())
                  clientGatewayCache.remove(userContext.appName)
                  ConnektLogger(LogFile.PROCESSORS).debug(s"APNSDispatcher apns-client destroyed ${userContext.appName}, since reconnect failed.")
                }
                //client.sendNotification(request).asScala //TODO: Observe number of errors and then enable retry if required.
                FastFuture.failed(nce)
          })(ec)
          .onComplete(responseTry ⇒ result.success(responseTry -> userContext))(ec)
        result.future
    }
  }

}
