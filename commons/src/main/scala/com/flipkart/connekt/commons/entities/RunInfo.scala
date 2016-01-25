package com.flipkart.connekt.commons.entities

import com.flipkart.connekt.commons.entities.Environments.Environment

/**
 * Created by kinshuk.bairagi on 22/01/16.
 */
object RunInfo {
    var ENV:Environment = Environments.NORMAL
}

object Environments extends Enumeration{
    type Environment = Environments.Value
    val TEST, NORMAL = Value
}
