package com.flipkart.connekt.commons.entities

/**
 *
 *
 * @author durga.s
 * @version 12/14/15
 */
sealed trait StencilEngine
case object GROOVY extends StencilEngine
case object VELOCITY extends StencilEngine
