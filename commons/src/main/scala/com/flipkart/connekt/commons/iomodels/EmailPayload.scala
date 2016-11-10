package com.flipkart.connekt.commons.iomodels


case class EmailPayload(from: EmailAddress, to: Set[EmailAddress], cc: Set[EmailAddress], bcc: Set[EmailAddress], data: EmailRequestData, expiryEpochInMillis: Long)

case class EmailPayloadEnvelope(messageId: String, appName: String, contextId: String, clientId: String, payload: EmailPayload, meta: Map[String, Any])
