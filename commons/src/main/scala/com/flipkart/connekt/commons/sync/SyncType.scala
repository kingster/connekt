package com.flipkart.connekt.commons.sync

/**
 * Created by kinshuk.bairagi on 02/03/16.
 */
object SyncType extends Enumeration {
   type SyncType = Value
   val CLIENT_ADD,TEMPLATE_CHANGE,AUTH_CHANGE = Value
}
