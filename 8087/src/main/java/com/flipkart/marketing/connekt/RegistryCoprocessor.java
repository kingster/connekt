package com.flipkart.marketing.connekt;

import org.apache.commons.logging.Log;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;

import java.io.IOException;

import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.commons.logging.LogFactory;

/**
 * Created by kinshuk.bairagi on 19/01/16.
 */
public class RegistryCoprocessor extends BaseRegionObserver {

    private static final Log logger = LogFactory.getLog(RegistryCoprocessor.class);

    @Override
    public void preDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, boolean writeToWAL) throws IOException {
        super.preDelete(e, delete, edit, writeToWAL);
    }

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e, Put put, WALEdit edit, boolean writeToWAL) throws IOException {

        try {
            KeyValue kv = new KeyValue(Bytes.toBytes("COPROCESSORROW"), Bytes.toBytes("colfam1"),Bytes.toBytes("COPROCESSOR: "+System.currentTimeMillis()),Bytes.toBytes("IT WORKED"));
            //put.add(kv);

//            String indexId = new String(put);
            //Object d = put.getRow();

            logger.info("werty");
        } catch (Exception ex) {
            logger.error(ex);
        }

        super.postPut(e, put, edit, writeToWAL);
    }
}


