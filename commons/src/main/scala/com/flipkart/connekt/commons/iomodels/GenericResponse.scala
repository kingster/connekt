package com.flipkart.connekt.commons.iomodels

/**
 *
 *
 * @author durga.s
 * @version 11/21/15
 */
case class GenericResponse(status: Int, request: Any, response: ResponseBody)

abstract class ResponseBody

case class Response(message: String, data: Any) extends ResponseBody

case class MulticastResponse(message: String, success: Map[String, List[String]], failure: List[String], missingInfo: List[String]) extends ResponseBody
