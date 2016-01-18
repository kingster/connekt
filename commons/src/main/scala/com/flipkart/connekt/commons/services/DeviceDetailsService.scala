package com.flipkart.connekt.commons.services

import com.flipkart.connekt.commons.dao.DaoFactory
import com.flipkart.connekt.commons.entities.DeviceDetails

/**
 * Created by kinshuk.bairagi on 16/01/16.
 */
object DeviceDetailsService {

  lazy val dao = DaoFactory.getDeviceDetailsDao

  def add(deviceDetails: DeviceDetails) = dao.add(deviceDetails.appName,deviceDetails)

  def update( deviceId:String, deviceDetails: DeviceDetails) = dao.update(deviceDetails.appName, deviceId, deviceDetails)

  def getByUserId(appName: String, userId: String) = dao.getByUserId(appName, userId)

  def get(appName: String, deviceId: String) = dao.get(appName, deviceId)


}
