package com.flipkart.connekt.commons.sync

/**
 * Created by kinshuk.bairagi.
 */
trait SyncDelegate {

  def onUpdate(_type: SyncType, args: List[AnyRef]): Any

}
