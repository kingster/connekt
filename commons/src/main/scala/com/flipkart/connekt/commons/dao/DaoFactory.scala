package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.HTableFactory
import com.flipkart.connekt.commons.helpers.HConnectionHelper
import com.typesafe.config.Config

/**
 *
 *
 * @author durga.s
 * @version 11/23/15
 */
object DaoFactory {

  var daoMap = Map[DaoType.Value, Dao]()
  var hTableFactory: HTableFactory = null

  def initHTableDaoFactory(hConnectionConfig: Config) = {
    hTableFactory = HConnectionHelper.createHbaseConnection(hConnectionConfig)

    daoMap += DaoType.DEVICE_DETAILS -> DeviceDetailsDao("fk-connekt-proto", hTableFactory)
    daoMap += DaoType.PN_REQUEST_INFO -> PNRequestDao("fk-connekt-pn-info", hTableFactory)
    daoMap += DaoType.CALLBACK_PN -> PNCallbackDao("fk-connekt-events", hTableFactory)
  }

  def shutdownHTableDaoFactory() = {
    Option(hTableFactory).foreach(_.shutdown())
  }

  def initMysqlTableDaoFactory(mysqlConnectionConfig: Config) = ???

  def getDeviceDetailsDao: DeviceDetailsDao = daoMap(DaoType.DEVICE_DETAILS).asInstanceOf[DeviceDetailsDao]

  def getRequestInfoDao: RequestDao = daoMap(DaoType.PN_REQUEST_INFO).asInstanceOf[PNRequestDao]

  def getPNCallbackDao: PNCallbackDao = daoMap(DaoType.CALLBACK_PN).asInstanceOf[PNCallbackDao]

  def getEmailCallbackDao: EmailCallbackDao = daoMap(DaoType.CALLBACK_EMAIL).asInstanceOf[EmailCallbackDao]
}

object DaoType extends Enumeration {
  val DEVICE_DETAILS, REQUEST_META, PN_REQUEST_INFO, CALLBACK_EMAIL, CALLBACK_PN = Value
}

