
#!/bin/bash

echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli-svc.list

apt-get update
apt-get install --yes --allow-unauthenticated infra-cli
apt-get install --yes --allow-unauthenticated python

reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-mysql-deployer --appkey fk-mysql-deployer > /etc/apt/sources.list.d/fk-mysql-deployer.list

apt-get update
apt-get install --yes --allow-unauthenticated fk-mysql-deployer

