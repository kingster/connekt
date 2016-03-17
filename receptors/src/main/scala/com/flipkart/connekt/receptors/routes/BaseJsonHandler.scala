/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.receptors.routes

import com.flipkart.connekt.receptors.wire.JsonFromEntityUnmarshaller

abstract class BaseJsonHandler extends BaseHandler with JsonFromEntityUnmarshaller
