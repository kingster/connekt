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

  def authorize( user: AppUser,tags:String*): Directive0 = {
    if(tags.map(tag => ServiceFactory.getAuthorisationService.isAuthorized(tag.toUpperCase, user.userId).getOrElse(false)).foldLeft(false)( _ || _))
      BasicDirectives.pass
    else
      RouteDirectives.reject(AuthorizationFailedRejection)
  }

}