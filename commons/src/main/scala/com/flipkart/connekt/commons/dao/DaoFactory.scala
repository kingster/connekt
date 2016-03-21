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

import com.couchbase.client.java.Bucket
import com.flipkart.connekt.commons.behaviors.{HTableFactory, MySQLFactory}
import com.flipkart.connekt.commons.connections.TConnectionProvider
import com.flipkart.connekt.commons.factories.{HTableFactoryWrapper, MySQLFactoryWrapper}
import com.flipkart.phantom.client.sockets.{PhantomClientSocket, PhantomSocketFactory}
import com.typesafe.config.Config

object DaoFactory {

  var connectionProvider: TConnectionProvider = null

  var daoMap = Map[DaoType.Value, Dao]()
  var mysqlFactoryWrapper: MySQLFactory = null

  var hTableFactory: HTableFactory = null

  var couchBaseCluster: com.couchbase.client.java.Cluster = null
  var couchbaseBuckets: Map[String, Bucket] = null

  var phantomClientSocket: PhantomClientSocket = null

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

    daoMap += DaoType.USER_INFO -> UserInfoDao("USER_INFO", mysqlFactoryWrapper)
    daoMap += DaoType.USER_CONFIG -> UserConfigurationDao("USER_CONFIG", mysqlFactoryWrapper)
    daoMap += DaoType.PRIVILEGE -> PrivDao("RESOURCE_PRIV", mysqlFactoryWrapper)
    daoMap += DaoType.STENCIL -> StencilDao("STENCIL_STORE", "STENCIL_HISTORY_STORE", "BUCKET_REGISTRY", mysqlFactoryWrapper)
    daoMap += DaoType.KEY_CHAIN -> KeyChainDao("DATA_STORE", mysqlFactoryWrapper)
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

  def initSpecterSocket(specterConfig: Config): PhantomClientSocket = {
    System.setProperty("org.newsclub.net.unix.library.path", specterConfig.getString("lib.path"))
    phantomClientSocket = PhantomSocketFactory.getInstance(specterConfig.getString("socket"))
    phantomClientSocket
  }

  def getDeviceDetailsDao: DeviceDetailsDao = daoMap(DaoType.DEVICE_DETAILS).asInstanceOf[DeviceDetailsDao]

  def getPNRequestDao: PNRequestDao = daoMap(DaoType.PN_REQUEST_INFO).asInstanceOf[PNRequestDao]

  def getPNCallbackDao: PNCallbackDao = daoMap(DaoType.CALLBACK_PN).asInstanceOf[PNCallbackDao]

  def getEmailCallbackDao: EmailCallbackDao = daoMap(DaoType.CALLBACK_EMAIL).asInstanceOf[EmailCallbackDao]

  def getPrivDao: PrivDao = daoMap(DaoType.PRIVILEGE).asInstanceOf[PrivDao]

  def getUserInfoDao: TUserInfo = daoMap(DaoType.USER_INFO).asInstanceOf[UserInfoDao]

  def getKeyChainDao: TKeyChainDao = daoMap(DaoType.KEY_CHAIN).asInstanceOf[KeyChainDao]

  def getUserConfigurationDao: TUserConfiguration = daoMap(DaoType.USER_CONFIG).asInstanceOf[UserConfigurationDao]

  def getStencilDao: TStencilDao = daoMap(DaoType.STENCIL).asInstanceOf[StencilDao]

}

object DaoType extends Enumeration {
  val DEVICE_DETAILS,
  REQUEST_META,
  PN_REQUEST_INFO,
  CALLBACK_EMAIL,
  CALLBACK_PN,
  PRIVILEGE,
  USER_INFO,
  USER_CONFIG,
  STENCIL,
  KEY_CHAIN = Value
}
