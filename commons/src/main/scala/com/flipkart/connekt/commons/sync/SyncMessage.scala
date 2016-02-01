package com.flipkart.connekt.commons.sync

/**
 * Created by kinshuk.bairagi on 02/02/16.
 */
case class SyncMessage(topic: SyncType, message: List[AnyRef])
