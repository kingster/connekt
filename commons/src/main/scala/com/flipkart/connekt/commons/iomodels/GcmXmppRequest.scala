package com.flipkart.connekt.commons.iomodels

import com.flipkart.connekt.commons.entities.GoogleCredential

/**
 * Created by subir.dey on 28/06/16.
 */
case class GcmXmppRequest (
  pnPayload:GCMXmppPNPayload,
  credential:GoogleCredential)
