package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0}
import akka.http.scaladsl.server.directives.{RouteDirectives, BasicDirectives}
import com.flipkart.connekt.commons.entities.AppUser

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
trait AuthorizationDirectives {

  def authorize( user: AppUser,tag:String): Directive0 = {
    if(true) //TODO" Implementation change
      BasicDirectives.pass
    else
      RouteDirectives.reject(AuthorizationFailedRejection)
  }

}