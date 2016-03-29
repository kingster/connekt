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
package com.flipkart.connekt.busybees.models


object MessageStatus {
  object GCMResponseStatus extends Enumeration {
    type GCMResponseStatus = Value

    val InvalidJsonError = Value("gcm_invalid_json_error")
    val AuthError = Value("gcm_auth_error")
    val Received = Value("gcm_received")
    val Error = Value("gcm_error")
    val InternalError = Value("gcm_internal_error")
  }

  object InternalStatus extends Enumeration {
    type InternalStatus = Value

    val RenderFailure = Value("connekt_render_failure")
    val MissingDeviceInfo = Value("connekt_missing_device")
    val SendError = Value("connekt_gcm_send_error")
    val ParseError = Value("connekt_gcm_response_parse_error")
    val TTLExpired = Value("connekt_ttl_expired")
    val StageError = Value("connekt_stage_error")
  }
}
