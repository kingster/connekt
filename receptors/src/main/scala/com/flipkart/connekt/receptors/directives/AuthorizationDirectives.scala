/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.receptors.directives

import akka.http.scaladsl.server.directives.{BasicDirectives, RouteDirectives}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0}
import com.flipkart.connekt.commons.entities.AppUser
import com.flipkart.connekt.commons.factories.ServiceFactory

import scala.util.Success

trait AuthorizationDirectives {

  def authorize(user: AppUser, tags: String*): Directive0 = {

    ServiceFactory.getAuthorisationService.isAuthorized(user.userId, tags: _*) match {
      case Success(authorize) if authorize =>
        BasicDirectives.pass
      case _ =>
        RouteDirectives.reject(AuthorizationFailedRejection)
    }
  }

}
