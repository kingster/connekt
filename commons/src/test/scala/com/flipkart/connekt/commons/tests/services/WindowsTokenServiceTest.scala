package com.flipkart.connekt.commons.tests.services

import com.flipkart.connekt.commons.services.WindowsTokenService
import com.flipkart.connekt.commons.tests.CommonsBaseTest

/**
 * @author aman.shrivastava on 16/02/16.
 */
class WindowsTokenServiceTest extends CommonsBaseTest {


  "Window Service" should "get Token" in {

    val wTS =  WindowsTokenService

    val r1 = new Runnable {
      override def run(): Unit = {
        try {
          wTS.refreshToken("RetailApp")
          Thread.sleep(2000)
          wTS.getToken("RetailApp") should not be None
        } catch {
          case e: Exception =>
            println("error = " + e.printStackTrace())
        }
      }
    }

    val r2 = new Runnable {
      override def run(): Unit = {
        Thread.sleep(2000)
        wTS.getToken("RetailApp") should not be None
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
