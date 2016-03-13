#!/usr/bin/env bash

curl -v -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/fk-connekt-kafka?appkey=connekt'  -d \
'[{
   "repoName": "fk-config-service-confd",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "fk-3p-kafka",
   "repoVersion": 1,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "oracle-java",
   "repoVersion": 4,
   "repoReferenceType": "EXACT"
},
{
   "repoName": "cosmos",
   "repoVersion": 2,
   "repoReferenceType": "HEAD"
},
{
   "repoName": "logsvc",
   "repoVersion": 17,
   "repoReferenceType": "HEAD"
}]' -H 'Content-Type: application/json'
