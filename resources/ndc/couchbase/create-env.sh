#!/usr/bin/env bash

curl -v -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/fk-connekt-couchbase?appkey=connekt'  -d \
'[
     {
                        "repoName": "couchbase4",
                        "repoVersion": 1,
                        "repoReferenceType": "HEAD"        
    }
]'   -H 'Content-Type: application/json'
