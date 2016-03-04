package com.flipkart.connekt.commons.sync

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.flipkart.connekt.commons.sync.SyncType.SyncType

/**
 * Created by kinshuk.bairagi on 02/02/16.
 */
case class SyncMessage(@JsonSerialize(using = classOf[SyncTypeToStringSerializer])
                       @JsonDeserialize(using = classOf[SyncTypeToStringDeserializer])
                       topic: SyncType,
                       message: List[AnyRef])
