#!/usr/bin/env bash

curl -v -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/fk-ckt-zookeeper?appkey=fk-ckt-zookeeper-ga'  -d \
'[{
   "repoName": "fk-config-service-confd",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "repo-cdh",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "oracle-java",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "hosts-populator",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "zookeeper-cdh",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
}]'   -H 'Content-Type: application/json'