/*
 *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
 *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
 *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
 *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
 *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
 *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
 *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
 *        ``˚¬ ⌐              ˚˚⌐´
 *
 *      Copyright © 2016 Flipkart.com
 */
package com.flipkart.connekt.commons.iomodels

object MessageStatus {

  object GCMResponseStatus extends Enumeration {
    type GCMResponseStatus = Value

    val InvalidJsonError = Value("gcm_invalid_json_error")
    val AuthError = Value("gcm_auth_error")
    val Received = Value("gcm_received")
    val Error = Value("gcm_error")
    val InternalError = Value("gcm_internal_error")
  }

  object WNSResponseStatus extends Enumeration {
    type WNSResponseStatus = Value

    val Received = Value("wns_received")
    val InvalidHeader = Value("wns_invalid_header")
    val InvalidMethod = Value("wns_invalid_method")
    val InvalidChannelUri = Value("wns_invalid_channel_uri")
    val InvalidDevice = Value("wns_invalid_device")
    val AuthError = Value("wns_auth_error")
    val ThrottleLimitExceeded = Value("wns_throttle_limit_exceeded")
    val ChannelExpired = Value("wns_channel_expired")
    val EntityTooLarge = Value("wns_entity_too_large")
    val PreConditionFailed = Value("wns_precondition_failed")
    val InternalError = Value("wns_internal_error")
  }

  object APNSResponseStatus extends Enumeration {
    type APNSResponseStatus = Value

    val Received = Value("apns_received")
    val TokenExpired = Value("apns_rejected_token_expired")
    val Rejected = Value("apns_rejected")
    val UnknownFailure = Value("apns_unknown_failure")
  }

  object InternalStatus extends Enumeration {
    type InternalStatus = Value

    val RenderFailure = Value("connekt_render_failure")
    val MissingDeviceInfo = Value("connekt_missing_device")
    val InvalidToken = Value("connekt_invalid_token")
    val GcmResponseParseError = Value("connekt_gcm_response_parse_error")
    val WnsResponseHandleError = Value("connekt_wns_response_handle_error")
    val TTLExpired = Value("connekt_ttl_expired")
    val StageError = Value("connekt_stage_error")
    val ProviderSendError = Value("connekt_provider_send_error")
    val Received = Value("connekt_received")
    val Rejected = Value("connekt_rejected")
  }
}
