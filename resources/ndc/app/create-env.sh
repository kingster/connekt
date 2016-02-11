#!/usr/bin/env bash

curl -v -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/fk-connekt-app?appkey=connekt'  -d \
'[
    {
        "repoName": "fk-pf-connekt",
        "repoVersion": 1,
        "repoReferenceType": "HEAD"        
    },
    {
        "repoName": "oracle-java",
        "repoVersion": 4,
        "repoReferenceType": "EXACT"        
    },
    {
        "repoReferenceType": "EXACT",
        "repoVersion": 62,
        "repoName": "specter"
    }
]'   -H 'Content-Type: application/json'