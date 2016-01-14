package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.behaviors.{HTableFactory, MySQLFactory}
import com.flipkart.connekt.commons.helpers.{HConnectionHelper, MySqlConnectionHelper}
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
  var mysqlFactoryWrapper: MySQLFactory = null

  def initHTableDaoFactory(hConnectionConfig: Config) = {
    hTableFactory = HConnectionHelper.createHbaseConnection(hConnectionConfig)

    daoMap += DaoType.DEVICE_DETAILS -> DeviceDetailsDao("fk-connekt-proto", hTableFactory)
    daoMap += DaoType.PN_REQUEST_INFO -> PNRequestDao(tableName = "fk-connekt-pn-info", hTableFactory = hTableFactory)
    daoMap += DaoType.CALLBACK_PN -> PNCallbackDao("fk-connekt-events", hTableFactory)
  }

  def shutdownHTableDaoFactory() = {
    Option(hTableFactory).foreach(_.shutdown())
  }


  def initMysqlTableDaoFactory(mysqlConnectionConfig: Config) = {
    mysqlFactoryWrapper = MySqlConnectionHelper.createMySqlConnection(mysqlConnectionConfig)

    daoMap += DaoType.USERINFO -> UserInfo("USER_INFO", mysqlFactoryWrapper)
    daoMap += DaoType.PRIVILEDGE -> PrivDao("RESOURCE_PRIV", mysqlFactoryWrapper)
    daoMap += DaoType.STENCIL -> StencilDao("STENCIL_STORE", mysqlFactoryWrapper)
  }

  def getDeviceDetailsDao: DeviceDetailsDao = daoMap(DaoType.DEVICE_DETAILS).asInstanceOf[DeviceDetailsDao]

  def getRequestInfoDao: PNRequestDao = daoMap(DaoType.PN_REQUEST_INFO).asInstanceOf[PNRequestDao]

  def getPNCallbackDao: PNCallbackDao = daoMap(DaoType.CALLBACK_PN).asInstanceOf[PNCallbackDao]

  def getEmailCallbackDao: EmailCallbackDao = daoMap(DaoType.CALLBACK_EMAIL).asInstanceOf[EmailCallbackDao]

  def getPrivDao: PrivDao = daoMap(DaoType.PRIVILEDGE).asInstanceOf[PrivDao]

  def getUserInfoDao: UserInfo = daoMap(DaoType.USERINFO).asInstanceOf[UserInfo]

  def getStencilDao: TStencilDao = daoMap(DaoType.STENCIL).asInstanceOf[StencilDao]
}

object DaoType extends Enumeration {
  val DEVICE_DETAILS,
  REQUEST_META,
  PN_REQUEST_INFO,
  CALLBACK_EMAIL,
  CALLBACK_PN,
  PRIVILEDGE,
  USERINFO,
  STENCIL = Value
}

