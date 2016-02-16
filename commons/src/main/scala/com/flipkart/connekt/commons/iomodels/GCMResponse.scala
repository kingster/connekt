package com.flipkart.connekt.commons.iomodels

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @author durga.s
 * @version 1/14/16
 */
abstract class GCMResponse
case class GCMProcessed(@JsonProperty("multicast_id") multicastId: Int, success: Int, failure: Int, @JsonProperty("canonical_ids") canonicalIds: Int, results: List[Map[String, String]]) extends GCMResponse
case class GCMSendFailure(error: String) extends GCMResponse
case class GCMRejected(@JsonProperty("status_code") statusCode: Int, error: String) extends GCMResponse