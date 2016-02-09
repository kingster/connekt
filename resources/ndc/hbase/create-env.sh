#!/usr/bin/env bash

curl -v -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/fk-ckt-hbase-hdfs?appkey=fk-ckt-hbase-dn-ga'  -d \
'[{
   "repoName": "oracle-java",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-config-service-confd",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "hosts-populator",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "repo-cdh",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "zookeeper-cdh",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-hdfs-builder",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-hdfs-user",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-hdfs-server-config",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-hdfs-config",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-hbase-config",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-hbase-builder",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-hbase-server-config",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
}
,
{
   "repoName": "cosmos",
   "repoVersion": 2,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "logsvc",
   "repoVersion": 17,
   "repoReferenceType": "HEAD"
}]'   -H 'Content-Type: application/json'