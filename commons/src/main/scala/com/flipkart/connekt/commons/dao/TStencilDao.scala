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
package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.{Bucket, Stencil, StencilComponents}

trait TStencilDao extends Dao {

  def getStencils(id: String, version: Option[String] = None): List[Stencil]

  def getStencilsByName(name: String, version: Option[String] = None): List[Stencil]

  //  def updateStencil(stencil: Stencil): Unit
  def writeStencil(stencil: Stencil): Unit

  def updateStencilWithIdentity(prevName: String, stencil: Stencil): Unit

  def deleteStencil(prevName: String, stencil: Stencil): Unit

  def getBucket(name: String): Option[Bucket]

  def writeBucket(bucket: Bucket): Unit

  def getStencilComponents(id: String): Option[StencilComponents]

  def writeStencilComponents(stencilComponents: StencilComponents): Unit
}

