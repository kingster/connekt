package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.directives.BasicDirectives

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
trait AuthorizationDirectives {

  def authorize(tag:String): Directive0 = {

    BasicDirectives.pass

  }

}