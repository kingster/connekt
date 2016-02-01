package com.flipkart.connekt.busybees.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape, SourceShape}
import com.flipkart.connekt.commons.tests.ConnektUTSpec
import org.scalatest.Ignore

/**
 *
 *
 * @author durga.s
 * @version 12/1/15
 */
@Ignore
class FlowSample extends ConnektUTSpec {

  lazy implicit val system = ActorSystem("sample-akka-stream")
  lazy implicit val materializer = ActorMaterializer()

  val flow = FlowGraph.create() { implicit builder: FlowGraph.Builder[Unit] =>
    import akka.stream.scaladsl.FlowGraph.Implicits._

    val in = Source(1 to 10)
    val out = Sink.foreach[Int](println)

    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))

    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    bcast ~> f4 ~> merge
    ClosedShape
  }

  val g = RunnableGraph.fromGraph(flow)

  "the created graph" should " run when materialized with inputs" in {
    val mat = g.run()
    println(mat.toString)
  }

  val pairs = Source.fromGraph(FlowGraph.create() { implicit b =>
    import akka.stream.scaladsl.FlowGraph.Implicits._

    // prepare graph elements
    val zip = b.add(Zip[Int, Int]())
    def ints = Source(() => Iterator.from(1))

    // connect the graph
    ints.filter(_ % 2 != 0) ~> zip.in0
    ints.filter(_ % 2 == 0) ~> zip.in1

    // expose port
    SourceShape(zip.out)
  })

  "flow created" should "run without exceptions" in {
    val firstPair = pairs.runWith(Sink.head)

  }
}
