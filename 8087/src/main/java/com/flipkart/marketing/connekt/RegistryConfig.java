package com.flipkart.marketing.connekt;

/**
 * Created by kinshuk.bairagi on 19/01/16.
 */
public interface RegistryConfig {

    static final String TABLE_REGISTRATION = "connekt-registry";
    static final String TABLE_USER_ID_SEC_IDX = "connekt-registry-user";
    static final String TABLE_TOKEN_SEC_IDX = "connekt-registry-token";

    static final String COL_F_REGISTRATION = "p";
    static final String COL_F_SEC_IDX = "s";

    static final String COL_Q_USER_ID = "userId";
    static final String COL_Q_TOKEN = "token";
    static final String COL_Q_DEVICE_ID = "deviceId";
}
