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
import com.flipkart.connekt.commons.connections.TConnectionProvider
import com.flipkart.connekt.commons.factories.{HTableFactory, MySQLFactory, THTableFactory, TMySQLFactory}
import com.flipkart.phantom.client.sockets.{PhantomClientSocket, PhantomSocketFactory}
import com.typesafe.config.Config

object DaoFactory {

  var connectionProvider: TConnectionProvider = null

  var daoMap = Map[DaoType.Value, Dao]()
  var mysqlFactoryWrapper: TMySQLFactory = null

  var hTableFactory: THTableFactory = null

  var couchBaseCluster: com.couchbase.client.java.Cluster = null
  var couchbaseBuckets: Map[String, Bucket] = null

  var phantomClientSocket: PhantomClientSocket = null

  def setUpConnectionProvider(provider: TConnectionProvider): Unit = {
    this.connectionProvider = provider
  }

  def initHTableDaoFactory(hConnectionConfig: Config) = {
    hTableFactory = new HTableFactory(hConnectionConfig, connectionProvider)

    daoMap += DaoType.DEVICE_DETAILS -> DeviceDetailsDao("connekt-registry", hTableFactory)
    daoMap += DaoType.PN_REQUEST_INFO -> PNRequestDao(tableName = "fk-connekt-pn-info", hTableFactory = hTableFactory)
    daoMap += DaoType.CALLBACK_PN -> PNCallbackDao("fk-connekt-events", hTableFactory)
  }

  def getHTableFactory = hTableFactory

  def shutdownHTableDaoFactory() = {
    daoMap.values.foreach(_.close())
    Option(hTableFactory).foreach(_.shutdown())
  }

  def initMysqlTableDaoFactory(config: Config) = {

    mysqlFactoryWrapper = new MySQLFactory(
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
    daoMap += DaoType.SUBSCRIPTION -> SubscriptionDao("SUBSCRIPTIONS", mysqlFactoryWrapper)
    daoMap += DaoType.STENCIL -> StencilDao("STENCIL_STORE", "STENCIL_HISTORY_STORE", "STENCILS_ENSEMBLE", "BUCKET_REGISTRY", mysqlFactoryWrapper)
    daoMap += DaoType.KEY_CHAIN -> KeyChainDao("DATA_STORE", mysqlFactoryWrapper)
  }

  def initCouchbaseCluster(config: Config) {
    couchBaseCluster = connectionProvider.createCouchBaseConnection(config.getString("clusterIpList").split(",").toList)
    couchbaseBuckets = Map()
  }

  def getCouchbaseBucket(name: String): Bucket = {
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

  def initReportingDao(bucket: Bucket): Unit = {
    daoMap += DaoType.STATS_REPORTING -> StatsReportingDao(bucket)
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

  def getStatsReportingDao: StatsReportingDao = daoMap(DaoType.STATS_REPORTING).asInstanceOf[StatsReportingDao]

  def getSubscriptionDao: TSubscriptionDao = daoMap(DaoType.SUBSCRIPTION).asInstanceOf[SubscriptionDao]
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
  STATS_REPORTING,
  SUBSCRIPTION,
  KEY_CHAIN = Value
}
