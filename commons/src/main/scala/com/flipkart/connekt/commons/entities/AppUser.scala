package com.flipkart.connekt.commons.entities

import org.apache.commons.lang.StringUtils

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
case class AppUser(userId: String, apiKey: String, groups: String, lastUpdatedTs: Long = System.currentTimeMillis(), updatedBy: String = StringUtils.EMPTY)