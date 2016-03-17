/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.sync

import com.flipkart.connekt.commons.sync.SyncType.SyncType

trait SyncDelegate {

  def onUpdate(_type: SyncType, args: List[AnyRef]): Any

}
