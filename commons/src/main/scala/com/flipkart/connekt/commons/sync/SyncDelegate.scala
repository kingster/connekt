package com.flipkart.connekt.commons.sync

import com.flipkart.connekt.commons.sync.SyncType.SyncType

/**
 * Created by kinshuk.bairagi.
 */
trait SyncDelegate {

  def onUpdate(_type: SyncType, args: List[AnyRef]): Any

}
