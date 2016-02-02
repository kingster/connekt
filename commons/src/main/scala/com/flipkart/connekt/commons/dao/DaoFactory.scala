package com.flipkart.connekt.commons.dao

import com.couchbase.client.java.Bucket
import com.flipkart.connekt.commons.behaviors.{HTableFactory, MySQLFactory}
import com.flipkart.connekt.commons.connections.TConnectionProvider
import com.flipkart.connekt.commons.factories.{MySQLFactoryWrapper, HTableFactoryWrapper}
import com.typesafe.config.Config

/**
  *
  *
  * @author durga.s
  * @version 11/23/15
  */
object DaoFactory {

  var connectionProvider: TConnectionProvider = null

  var daoMap = Map[DaoType.Value, Dao]()
  var mysqlFactoryWrapper: MySQLFactory = null

  var hTableFactory: HTableFactory = null

  var couchBaseCluster: com.couchbase.client.java.Cluster = null
  var couchbaseBuckets: Map[String, Bucket] = null

  def setUpConnectionProvider(provider: TConnectionProvider): Unit = {
    this.connectionProvider = provider
  }

  def initHTableDaoFactory(hConnectionConfig: Config) = {
    hTableFactory = new HTableFactoryWrapper(hConnectionConfig, connectionProvider)

    daoMap += DaoType.DEVICE_DETAILS -> DeviceDetailsDao("connekt-registry", hTableFactory)
    daoMap += DaoType.PN_REQUEST_INFO -> PNRequestDao(tableName = "fk-connekt-pn-info", hTableFactory = hTableFactory)
    daoMap += DaoType.CALLBACK_PN -> PNCallbackDao("fk-connekt-events", hTableFactory)
  }

  def shutdownHTableDaoFactory() = {
    Option(hTableFactory).foreach(_.shutdown())
  }

  def initMysqlTableDaoFactory(config: Config) = {

    mysqlFactoryWrapper = new MySQLFactoryWrapper(
      host = config.getString("host"),
      database = config.getString("database"),
      username = config.getString("username"),
      password = config.getString("password"),
      poolProps = config.getConfig("poolProps"),
      connectionProvider
    )

    daoMap += DaoType.USERINFO -> UserInfo("USER_INFO", mysqlFactoryWrapper)
    daoMap += DaoType.PRIVILEDGE -> PrivDao("RESOURCE_PRIV", mysqlFactoryWrapper)
    daoMap += DaoType.STENCIL -> StencilDao("STENCIL_STORE", "STENCIL_HISTORY_STORE", "BUCKET_REGISTRY", mysqlFactoryWrapper)
  }

  def initCouchbaseCluster(config: Config) {
    couchBaseCluster = connectionProvider.createCouchBaseConnection(config.getString("clusterIpList").split(",").toList)
    couchbaseBuckets = Map()
  }

  def getCouchbaseBucket(name: String = "Default"): Bucket = {
    couchbaseBuckets.get(name) match {
      case Some(x) => x
      case None =>
        val bucket = couchBaseCluster.openBucket(name)
        couchbaseBuckets += name -> bucket
        bucket
    }
  }

  def shutdownCouchbaseCluster() {
    Option(couchBaseCluster).foreach(_.disconnect())
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

