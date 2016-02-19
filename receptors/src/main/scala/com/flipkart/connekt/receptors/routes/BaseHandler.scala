package com.flipkart.connekt.receptors.routes

import akka.http.scaladsl.server.Directives
import com.flipkart.connekt.receptors.directives.{AsyncDirectives, AuthenticationDirectives, AuthorizationDirectives, HeaderDirectives}

/**
 * Created by kinshuk.bairagi on 19/02/16.
 */
abstract trait BaseHandler extends Directives with HeaderDirectives with AuthenticationDirectives with AuthorizationDirectives with AsyncDirectives {
}
