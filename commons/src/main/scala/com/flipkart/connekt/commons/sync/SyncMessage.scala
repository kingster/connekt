/*
 * Copyright (C) 2016 Flipkart.com <http://www.flipkart.com>
 */
package com.flipkart.connekt.commons.sync

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.flipkart.connekt.commons.sync.SyncType.SyncType

case class SyncMessage(@JsonSerialize(using = classOf[SyncTypeToStringSerializer])
                       @JsonDeserialize(using = classOf[SyncTypeToStringDeserializer])
                       topic: SyncType,
                       message: List[AnyRef])
