/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.entities

import com.flipkart.connekt.commons.utils.StringUtils

/**
 * @author aman.shrivastava on 08/02/16.
 */
case class WNSCredential(secureId: String, clientSecret: String) {
  def isEmpty:Boolean = {
    StringUtils.isNullOrEmpty(secureId) && StringUtils.isNullOrEmpty(clientSecret)
  }
}
