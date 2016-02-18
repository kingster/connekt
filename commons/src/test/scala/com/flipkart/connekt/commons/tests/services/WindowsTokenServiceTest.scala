package com.flipkart.connekt.commons.tests.services

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.flipkart.connekt.commons.services.WindowsTokenService
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * @author aman.shrivastava on 16/02/16.
 */
class WindowsTokenServiceTest extends CommonsBaseTest with ScalatestRouteTest {

//  implicit val x = system.dispatcher

  val wTS =  WindowsTokenService

  "Window Service" should "get Token" in {

    val r1 = new Runnable {
      override def run(): Unit = {
        try {
          wTS.refreshToken("abc")
          Thread.sleep(6000)
          wTS.getToken("abc") should not be None
        } catch {
          case e: Exception =>
            println("error = " + e.printStackTrace())
        }
      }
    }

    val r2 = new Runnable {
      override def run(): Unit = {
          wTS.getToken("abc") should not be None
      }
    }


    val t1 = new Thread(r1)
    val t2 = new Thread(r2)
    t1.start()
    t2.start()
    t1.join()
    t2.join()
  }


}
