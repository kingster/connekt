package com.flipkart.connekt.commons.iomodels

/**
 *
 *
 * @author durga.s
 * @version 1/14/16
 */
abstract class GCMResponse
case class GCMProcessed(multicastId: Int, success: Int, failure: Int, canonicalIds: Int, results: List[Map[String, String]]) extends GCMResponse
case class GCMSendFailure(error: String) extends GCMResponse
case class GCMRejected(statusCode: Int, error: String) extends GCMResponse