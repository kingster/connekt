#!/bin/bash

#setup infra-cli and default srcs.list
echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli-svc.list
apt-get update
apt-get install --yes --allow-unauthenticated infra-cli

#setup your package
reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-connekt-app --appkey connekt > /etc/apt/sources.list.d/fk-connekt-app.list
reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-connekt-commons --appkey connekt > /etc/apt/sources.list.d/fk-connekt-commons.list
reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name openssl --appkey openssl > /etc/apt/sources.list.d/openssl.list

sudo apt-get update

sudo apt-get install --yes --allow-unauthenticated fk-pf-connekt
sudo /etc/init.d/fk-pf-connekt set-env ndc

#sudo /etc/init.d/fk-pf-connekt start receptors

bash -c "echo 'connekt-app' > /etc/default/cosmos-service"


echo 'team_name="Connekt"' > /etc/default/nsca_wrapper
echo 'nagios_server_ip="10.47.2.198"' >> /etc/default/nsca_wrapper

sudo apt-get install --yes --allow-unauthenticated fk-nagios-common --reinstall || true


#specter
echo 'export DART_CONFIG_SVC_BUCKETS=prod-fdpingestion-specter' > /etc/default/fk-bigfoot-dart.env
# sudo apt-get install --yes --allow-unauthenticated specter  #we don't trust specter.
