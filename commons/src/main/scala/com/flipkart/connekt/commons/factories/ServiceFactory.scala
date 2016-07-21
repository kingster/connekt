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
package com.flipkart.connekt.commons.factories

import com.flipkart.connekt.commons.dao._
import com.flipkart.connekt.commons.helpers.KafkaProducerHelper
import com.flipkart.connekt.commons.services.{KeyChainService, _}
import com.typesafe.config.Config
import org.apache.hadoop.hbase.client.Connection

object ServiceFactory {

  var serviceCache = Map[ServiceType.Value, TService]()

  def initPNMessageService(requestDao: PNRequestDao, userConfiguration: TUserConfiguration, queueProducerHelper: KafkaProducerHelper, kafkaConsumerConfig: Config, schedulerService: SchedulerService) = {
    serviceCache += ServiceType.PN_MESSAGE -> new MessageService(requestDao, userConfiguration, queueProducerHelper, kafkaConsumerConfig, schedulerService)
  }

  def initCallbackService(emailCallbackDao: EmailCallbackDao, pnCallbackDao: PNCallbackDao, pullCallbackDao: PullCallbackDao, pullRequestDao: PullRequestDao, requestInfoDao: PNRequestDao, emailRequestDao: EmailRequestDao, queueProducerHelper: KafkaProducerHelper) = {
    serviceCache += ServiceType.CALLBACK -> new CallbackService(pnCallbackDao, pullCallbackDao, emailCallbackDao, requestInfoDao, pullRequestDao,  emailRequestDao, queueProducerHelper)
  }

  def initPullMessageService(requestDao: PullRequestDao, userConfiguration: TUserConfiguration) = {
    serviceCache += ServiceType.PULL_MESSAGE -> new MessageService(requestDao, userConfiguration, null, null, null)
  }

  def initAuthorisationService(priv: PrivDao, userInfo: TUserInfo) = {
    serviceCache += ServiceType.AUTHORISATION -> new AuthorisationService(priv, userInfo)
    serviceCache += ServiceType.USER_INFO -> new UserInfoService(userInfo)
  }

  def initStorageService(dao: TKeyChainDao) = {
    serviceCache += ServiceType.KEY_CHAIN -> new KeyChainService(dao)
  }

  def initStatsReportingService(dao : StatsReportingDao): Unit ={
    val instance = new ReportingService(dao)
    instance.init()
    serviceCache += ServiceType.STATS_REPORTING -> instance
  }

  def initSchedulerService(hConnection: Connection): Unit ={
    val instance = new SchedulerService(hConnection)
    serviceCache += ServiceType.SCHEDULER -> instance
  }

  def initStencilService(dao: TStencilDao) = serviceCache += ServiceType.STENCIL -> new StencilService(dao)

  def getSchedulerService = serviceCache(ServiceType.SCHEDULER).asInstanceOf[SchedulerService]

  def getPNMessageService = serviceCache(ServiceType.PN_MESSAGE).asInstanceOf[TMessageService]

  def getPullMessageService = serviceCache(ServiceType.PULL_MESSAGE).asInstanceOf[TMessageService]

  def getCallbackService = serviceCache(ServiceType.CALLBACK).asInstanceOf[TCallbackService]

  def getAuthorisationService = serviceCache(ServiceType.AUTHORISATION).asInstanceOf[TAuthorisationService]

  def getUserInfoService  = serviceCache(ServiceType.USER_INFO).asInstanceOf[UserInfoService]

  def getKeyChainService = serviceCache(ServiceType.KEY_CHAIN).asInstanceOf[TStorageService]

  def getReportingService = serviceCache(ServiceType.STATS_REPORTING).asInstanceOf[ReportingService]

  def getStencilService = serviceCache(ServiceType.STENCIL).asInstanceOf[TStencilService]

}

object ServiceType extends Enumeration {
  val PN_MESSAGE, TEMPLATE, CALLBACK, USER_INFO, AUTHORISATION, KEY_CHAIN, STATS_REPORTING, STENCIL, SCHEDULER, PULL_MESSAGE = Value
}
