package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0}
import akka.http.scaladsl.server.directives.{RouteDirectives, BasicDirectives}
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.ServiceFactory

/**
 *
 *
 * @author durga.s
 * @version 11/22/15
 */
trait AuthorizationDirectives {

  def authorize( user: AppUser,tag:String): Directive0 = {
    if(ServiceFactory.getAuthorisationService.isAuthorized(tag, user.userId).getOrElse(false))
      BasicDirectives.pass
    else
      RouteDirectives.reject(AuthorizationFailedRejection)
  }

}