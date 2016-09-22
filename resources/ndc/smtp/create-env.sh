#!/usr/bin/env bash

curl -vvv -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/bcc-smtp?appkey=connekt'  -d \
'[
    {
        "repoName": "bcc-smtp",
        "repoVersion": 1,
        "repoReferenceType": "HEAD"        
    }
]'   -H 'Content-Type: application/json'
