#!/usr/bin/env bash

reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-connekt-app --appkey connekt | sudo tee /etc/apt/sources.list.d/fk-connekt-app.list
sudo apt-get update

sudo apt-get install --yes --allow-unauthenticated fk-pf-connekt

#sudo /etc/init.d/fk-pf-connekt start receptors