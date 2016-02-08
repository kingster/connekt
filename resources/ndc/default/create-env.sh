#!/usr/bin/env bash


curl -v -X PUT 'http://repo-svc-app-0001.nm.flipkart.com:8080/env/fk-connekt-commons?appkey=connekt'  -d \
'[
    {
        "repoName": "alertz-nagios-common",
        "repoReferenceType": "HEAD",
        "repoVersion": 2
    },
    {
        "repoName": "alertz-nsca-wrapper",
        "repoReferenceType": "HEAD",
        "repoVersion": 1
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
    },
    {
        "repoName": "hosts-populator",
        "repoVersion": 15,
        "repoReferenceType": "HEAD"
    },
    {
        "repoName": "fk-config-service-confd",
        "repoVersion": 1,
        "repoReferenceType": "HEAD"
    }
]'   -H 'Content-Type: application/json'