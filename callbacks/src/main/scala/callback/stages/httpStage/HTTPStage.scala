package callback.stages.httpStage

import java.net.URL

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{GraphDSL, MergePreferred, Sink}
import akka.stream.{ActorMaterializer, SinkShape}
import com.flipkart.connekt.commons.iomodels.CallbackEvent
import akka.stream.scaladsl.GraphDSL.Implicits._
import callback.commons.GenericFlow
import callback.stages.ClientTopology
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.flipkart.connekt.commons.entities.{Channel, HTTPEndpoint, Subscription}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
  * Created by harshit.sinha on 13/06/16.
  */
class HTTPStage(clientTopology: ClientTopology)(implicit am: ActorMaterializer,sys: ActorSystem, ec: ExecutionContext) {


  val url = new URL(clientTopology.subscription.endpoint.asInstanceOf[HTTPEndpoint].url)
  val httpCachedClient = Http().cachedHostConnectionPool[HttpCallbackTracker](url.getHost, url.getPort)
  var currentShutdown = 0
  var thresholdShutdown = clientTopology.subscription.shutdownThreshold

  def getHttpSink(): Sink[CallbackEvent, NotUsed] = {

    val convertStage = new GenericFlow[CallbackEvent,(HttpRequest,HttpCallbackTracker)](converter)
    val responseEvaluatorStage = new GenericFlow[(Try[HttpResponse],HttpCallbackTracker),Either[HttpResponse, HttpCallbackTracker]](responseEvaluator)
    val responseResultHandlerStage = new ResponseResultHandler()

    Sink.fromGraph(GraphDSL.create() { implicit b =>

      val convertStaged = b.add(convertStage)
      val mergePreferredStaged = b.add(MergePreferred[(HttpRequest,HttpCallbackTracker)](1))
      val responseEvaluatorStaged = b.add(responseEvaluatorStage)
      val responseResultHandlerStaged = b.add(responseResultHandlerStage)
      val sinkDeliveredStaged = b.add(Sink.foreach(println))
      val sinkDiscardedStaged = b.add(Sink.foreach(println))

      convertStaged ~> mergePreferredStaged.in(0)
      mergePreferredStaged.out ~> httpCachedClient ~> responseEvaluatorStaged ~> responseResultHandlerStaged.in
      responseResultHandlerStaged.out1 ~> sinkDeliveredStaged
      responseResultHandlerStaged.out0 ~> mergePreferredStaged.preferred
      responseResultHandlerStaged.out2 ~> sinkDiscardedStaged

      SinkShape(convertStaged.in)

    })

  }


  def converter(event: CallbackEvent): (HttpRequest, HttpCallbackTracker) =
  {
    val objMapper = new ObjectMapper() with ScalaObjectMapper
    objMapper.registerModules(Seq(DefaultScalaModule): _*)
    objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val payload = objMapper.writeValueAsString(event)
    val httpEntity = HttpEntity(ContentTypes.`application/json`, payload)
    val httpRequest = HttpRequest( method = HttpMethods.POST ,uri = url.getPath ,entity = httpEntity)
    val callbackTracker = new HttpCallbackTracker(payload,0,false,url.getPath)
    (httpRequest, callbackTracker)
  }




  /**
    * this function takes the response provided by the httpCachedClient and evaluates the response
    * if the response is 200 ok then it emits the HttpResponse to the responseHandler
    * else it then evaluate the failure and update the state of CallbackTracker object and sends it
    * to the response handler
    *
    * @param responseObject          the response we get from httpCachedClient
    * @return                        a httpResponse on success  and a Tracker with updated state on failure
    */

  def responseEvaluator(responseObject: (Try[HttpResponse],HttpCallbackTracker)) : Either[HttpResponse, HttpCallbackTracker] =
  {
    currentShutdown = currentShutdown + 1
    val response = responseObject._1
    val callbackTracker = responseObject._2

    response match {
      case Success(r) => {
        response.get.entity.dataBytes.runWith(Sink.ignore)
        r.status.intValue() match {
          case 200 => {
            currentShutdown = 0
            Left(response.get)
          }
          case _ => {
            if( currentShutdown > thresholdShutdown && clientTopology.active) clientTopology.clientTopologyManager.stopTopology(clientTopology.subscription)
            if(callbackTracker.error == clientTopology.retryLimit) return Right(new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, true, callbackTracker.serverPath))
            else Right (new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, false, callbackTracker.serverPath))
          }
        }
      }
      case Failure(e) => {
        if( currentShutdown > thresholdShutdown && clientTopology.active) clientTopology.clientTopologyManager.stopTopology(clientTopology.subscription)
        if(callbackTracker.error == clientTopology.retryLimit) return Right(new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, true, callbackTracker.serverPath))
        else Right (new HttpCallbackTracker(callbackTracker.payload, callbackTracker.error+1, false, callbackTracker.serverPath))
      }
    }

  }


}
