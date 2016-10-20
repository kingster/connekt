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
package com.flipkart.connekt.commons.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import fkint.mp.connekt.DeviceDetails

/**
 *
 *
 * @author durga.s
 * @version 10/13/16
 */
object TestSomething {
  case class Foo(a: String, b: Int)

  abstract class Behaviour {
    def x = {}
  }

  trait FooBar {}

  val objMapper = new ObjectMapper() with ScalaObjectMapper
  objMapper.registerModules(Seq(DefaultScalaModule): _*)

  def main (args: Array[String]) {
    val dataClass = DeviceDetails.getClass
    val packageTokens = dataClass.getPackage().getName().replaceAll("com.flipkart.seraph\\.", "").split("\\.", 3)
    val uriBuilder = new StringBuilder()
    val uri = uriBuilder.append(packageTokens(0)).append("/").append(packageTokens(1)).append("/").append(packageTokens(2)).append("/").append(dataClass.getSimpleName()).toString()

    println(uri)
  }

}
