/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.databind.node.ObjectNode

case class PNRequestData(data: ObjectNode) extends ChannelRequestData
