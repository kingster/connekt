/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonInclude


case class GCMPayloadEnvelope(messageId: String, deviceId: List[String], appName: String, gcmPayload: GCMPayload)

abstract class GCMPayload

case class GCMPNPayload(registration_ids: List[String],
                        @JsonInclude(JsonInclude.Include.NON_NULL) delay_while_idle: Option[Boolean],
                        data: Any,
                        @JsonInclude(JsonInclude.Include.NON_NULL) time_to_live: Option[Long] = None,
                        @JsonInclude(JsonInclude.Include.NON_NULL) dry_run: Option[Boolean] = None) extends GCMPayload

case class OpenWebGCMPayload(registration_ids: List[String],
                             @JsonInclude(JsonInclude.Include.NON_NULL) dry_run: Option[Boolean] = None) extends GCMPayload
