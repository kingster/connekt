package callback.stages.httpStage

import akka.http.scaladsl.model.{HttpEntity, _}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FanOutShape3, Inlet, Outlet}
import scala.concurrent.ExecutionContext

/**
  * Created by harshit.sinha on 04/06/16.
  */

/**
  *  this class takes the result from response evaluator and if the response is ok sent it out through
  *  sinkOutlet to be feed into the sink
  *  otherwise if the passiveState is true then it will be sent out through passiveOutlet waiting to be
  *  feed into sideline queue
  *  otherwise pushes it out of retryOutlet from where it joins the stream through merge.preferred stage
  *
  * @param ec implict executioner
  */

class ResponseResultHandler(implicit val ec: ExecutionContext) extends GraphStage[FanOutShape3[Either[HttpResponse,HttpCallbackTracker],(HttpRequest,HttpCallbackTracker), HttpResponse,HttpCallbackTracker]] {

  val in = Inlet[Either[HttpResponse, HttpCallbackTracker]]("input")
  val retryOutlet = Outlet[(HttpRequest, HttpCallbackTracker)]("error.out")
  val sinkOutlet = Outlet[HttpResponse]("fine.out")
  val discardOutlet = Outlet[HttpCallbackTracker]("discardedEvents.out")

  override def shape = new FanOutShape3(in, retryOutlet, sinkOutlet, discardOutlet)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val responseEvaluatorResult = grab(in)
        responseEvaluatorResult match {
          case Left(httpResponse) => push(sinkOutlet, httpResponse)
          case Right(callbackTracker) => if (callbackTracker.discarded == true) push(discardOutlet, callbackTracker)
          else {
            val httpEntity = HttpEntity(ContentTypes.`application/json`, callbackTracker.payload)
            val httpRequest = HttpRequest(method = HttpMethods.POST, uri = callbackTracker.serverPath, entity = httpEntity)
            push(retryOutlet, (httpRequest, callbackTracker))
          }
        }
      }
    })


    setHandler(sinkOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

    setHandler(retryOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

    setHandler(discardOutlet, new OutHandler {
      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
    })

  }
}

