#!/usr/bin/env bash

echo "deb http://10.47.2.22:80/repos/couchbase3/1 /" | sudo tee /etc/apt/sources.list.d/fk-couchbase.list

sudo apt-get update
sudo apt-get install --yes --allow-unauthenticated couchbase-server

mkdir -p /var/storage/couchbase/data
mkdir -p /var/storage/couchbase/index

chown -R couchbase:couchbase /var/storage/couchbase/data
chown -R couchbase:couchbase /var/storage/couchbase/index
