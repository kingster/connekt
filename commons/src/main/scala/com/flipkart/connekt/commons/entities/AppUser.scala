package com.flipkart.connekt.commons.entities

import javax.persistence.Column

import org.apache.commons.lang.StringUtils

/**
 *
 *
 * @author durga.s
 * @version 12/10/15
 */
case class AppUser(@Column(name = "userId") userId: String,
                   @Column(name = "apikey") apiKey: String,
                   @Column(name = "groups") groups: String,
                   @Column(name = "lastUpdatedTs") lastUpdatedTs: Long = System.currentTimeMillis(),
                   @Column(name = "updatedBy") updatedBy: String = StringUtils.EMPTY)