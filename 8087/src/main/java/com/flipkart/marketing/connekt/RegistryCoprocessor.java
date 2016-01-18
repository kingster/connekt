package com.flipkart.marketing.connekt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by kinshuk.bairagi on 19/01/16.
 */
public class RegistryCoprocessor extends BaseRegionObserver {

    private static final Log logger = LogFactory.getLog(RegistryCoprocessor.class);

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        logger.info("RegistryCoprocessor start");
    }

    @Override
    public void preDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, boolean writeToWAL) throws IOException {
        super.preDelete(e, delete, edit, writeToWAL);
    }


    @Override
    public void prePut(ObserverContext<RegionCoprocessorEnvironment> contx, Put put, WALEdit edit, boolean writeToWAL) throws IOException {
        try {
            byte[] tableName = contx.getEnvironment().getRegion().getTableDesc().getName();
            String tableNameStr = Bytes.toString(tableName);
            if(Arrays.asList(RegistryConfig.TABLES).contains(tableNameStr)){


                logger.info("werty");
            }
        } catch (Exception ex) {
            logger.error(ex);
        }

        super.postPut(contx, put, edit, writeToWAL);
    }
}


