package com.flipkart.connekt.commons.sync

import com.flipkart.connekt.commons.sync.SyncType.SyncType

/**
 * Created by kinshuk.bairagi on 02/02/16.
 */
case class SyncMessage(topic: SyncType, message: List[AnyRef])
