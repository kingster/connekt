package com.flipkart.connekt.busybees.utils

import java.io.{BufferedReader, InputStreamReader, PipedInputStream, PipedOutputStream}

import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer
import akka.stream.io.OutputStreamSink
import akka.stream.javadsl.Source
import akka.util.ByteString

/**
 *
 *
 * @author durga.s
 * @version 11/29/15
 */
object ResponseUtils {

/*
  implicit val system = ActorSystem("flowtest")
  implicit val mater = ActorMaterializer()
*/

  def StreamToString(source:Source[ByteString, AnyRef])(implicit fm: Materializer): String ={
    val pipedIn = new PipedInputStream()
    val pipedOut = new PipedOutputStream(pipedIn)
    val flow = source.to(OutputStreamSink(() => pipedOut))
    flow.run(fm)

    val reader = new BufferedReader(new InputStreamReader(pipedIn))
    reader.readLine
  }

  implicit class responseParser(val r: HttpResponse)(implicit fm: Materializer) {
    def getResponseMessage = {
      val rStream = r.entity.getDataBytes()
      val pipedIn = new PipedInputStream()
      val pipedOut = new PipedOutputStream(pipedIn)
      val flow = rStream.to(OutputStreamSink(() => pipedOut))
      flow.run(fm)

      val reader = new BufferedReader(new InputStreamReader(pipedIn))
      reader.readLine
    }
  }
}
