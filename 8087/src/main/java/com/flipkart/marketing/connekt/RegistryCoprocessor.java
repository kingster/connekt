package com.flipkart.marketing.connekt;

import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;

public class RegistryCoprocessor extends BaseRegionObserver implements RegistryConfig {

//    private static final Log logger = LogFactory.getLog(RegistryCoprocessor.class);
//    private static final ExecutorService execService = Executors.newCachedThreadPool();
//
//    @Override
//    public void start(CoprocessorEnvironment env) throws IOException {
//        logger.info("RegistryCoprocessor start");
//    }
//
//    @Override
//    public void preDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, boolean writeToWAL) throws IOException {
//        super.preDelete(e, delete, edit, writeToWAL);
//    }
//
//
//    @Override
//    public void prePut(ObserverContext<RegionCoprocessorEnvironment> ctx, Put put, WALEdit edit, boolean writeToWAL) throws IOException {
//
////        String tableName = Bytes.toString(ctx.getEnvironment().getRegion().getRegionInfo().getTableName());
////        if(tableName.equals(TABLE_REGISTRATION)) {
////            Put userIdSecIdx = putSecondaryIdx(ctx, put, TABLE_USER_ID_SEC_IDX, COL_Q_USER_ID, COL_Q_DEVICE_ID);
////            if(null != userIdSecIdx)
////                if(null == putSecondaryIdx(ctx, put, TABLE_TOKEN_SEC_IDX, COL_Q_TOKEN, COL_Q_DEVICE_ID)) {
////                    removeInconsistentIdx(ctx, userIdSecIdx.getRow());
////                    throw new RuntimeException("Adding secondary index failed for " + new String(put.getRow()));
////                }
////        }
//    }
//
//    private Put putSecondaryIdx(ObserverContext<RegionCoprocessorEnvironment> ctx, Put registration, String secIndexTable, String secColQualifier, String primaryColQualifier) {
//
////        HTableInterface hTable = null;
////        try {
////            byte[] secColValue = registration.get(COL_F_REGISTRATION.getBytes(StandardCharsets.UTF_8), secColQualifier.getBytes(StandardCharsets.UTF_8)).get(0).getValue();
////            byte[] primaryColValue = registration.get(COL_F_REGISTRATION.getBytes(StandardCharsets.UTF_8), primaryColQualifier.getBytes(StandardCharsets.UTF_8)).get(0).getValue();
////
////            Put secPut = new Put(Utils.shaHash(secColValue));
////            secPut.add(COL_F_SEC_IDX.getBytes(StandardCharsets.UTF_8), primaryColQualifier.getBytes(StandardCharsets.UTF_8), primaryColValue);
////            hTable = ctx.getEnvironment().getTable(TABLE_REGISTRATION.getBytes(), execService);
////            hTable.put(secPut);
////            return secPut;
////
////        } catch (Exception e) {
////            logger.error(String.format("Secondary Idx[%s] Put Failed: %s", secIndexTable, e.getMessage()), e);
////        } finally {
////            if(null != hTable)
////                try {
////                    hTable.close();
////                } catch (IOException e) {
////                    logger.error(e.getMessage(), e);
////                }
////        }
//        return null;
//    }
//
//    private void removeInconsistentIdx(ObserverContext<RegionCoprocessorEnvironment> ctx, byte[] rowKey) {
//
//        HTableInterface hTable = null;
//        try {
//            hTable = ctx.getEnvironment().getTable(TABLE_REGISTRATION.getBytes(), execService);
//            hTable.delete(new Delete(rowKey));
//
//        } catch (Exception e) {
//            logger.error("Inconsistent Secondary Idx removal failed: " + e.getMessage(), e);
//        } finally {
//            if(null != hTable)
//                try {
//                    hTable.close();
//                } catch (IOException e) {
//                    logger.error(e.getMessage(), e);
//                }
//        }
//    }
}


