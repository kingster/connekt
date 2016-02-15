package com.flipkart.connekt.commons.dao

import com.flipkart.connekt.commons.entities.AppUserConfiguration
import com.flipkart.connekt.commons.entities.Channel.Channel


trait TUserConfiguration {

  def getUserConfiguration(userId: String, channel: Channel): Option[AppUserConfiguration]

  def addUserConfiguration(config: AppUserConfiguration)

}
