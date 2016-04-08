#!/usr/bin/env bash

curl -v -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/fk-pf-azkaban?appkey=fk-pf-azkaban'  -d \
'[
    {
        "repoName": "fk-config-service-confd",
        "repoVersion": 1,
        "repoReferenceType": "HEAD"
    },
    {
        "repoName": "oracle-java",
        "repoVersion": 4,
        "repoReferenceType": "EXACT"
    },
    {
        "repoName": "fk-pf-azkaban",
        "repoVersion": 1,
        "repoReferenceType": "HEAD"
    },
    {
        "repoName": "fk-3p-mail",
        "repoVersion": 1,
        "repoReferenceType": "HEAD"
    }
]'   -H 'Content-Type: application/json'
