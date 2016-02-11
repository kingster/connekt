package com.flipkart.connekt.commons.tests.metrics

import com.flipkart.connekt.commons.tests.ConnektUTSpec
import com.flipkart.metrics.{Instrumented, Timed}

/**
 * Created by kinshuk.bairagi on 11/02/16.
 */
class MetricsTest  extends ConnektUTSpec with Instrumented {


  "Metrics Test " should "meter" in {

    @Timed("meter-test")
    def someSleep(x:String):String =  {
      Thread.sleep(2)
      x
    }


    @Timed("meter-test2")
    def someSleep2(x:String):Unit =  {
      Thread.sleep(2)
      Some(x)
      val d = 1
    }


    1 to 100 foreach(x => {
      someSleep("i")
      someSleep2("X")
    })


    Thread.sleep(40000)

    assert(true, true)

  }


}
