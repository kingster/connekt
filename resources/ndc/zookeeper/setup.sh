#!/usr/bin/env bash
#!/bin/bash
echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli.list
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated infra-cli
reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-ckt-zookeeper --appkey fk-ckt-zookeeper-ga > /etc/apt/sources.list.d/fk-ckt-zookeeper.list
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated fk-cdh-repo
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated python

reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-connekt-commons --appkey connekt > /etc/apt/sources.list.d/fk-connekt-commons.list

sudo apt-get update

echo 'team_name="Connekt"' > /etc/default/nsca_wrapper
echo 'nagios_server_ip="10.47.2.198"' >> /etc/default/nsca_wrapper

sudo apt-get install --yes --allow-unauthenticated fk-nagios-common --reinstall


echo "n
p
1


w" | fdisk /dev/vdb
mkfs.ext4 /dev/vdb1
mkdir -p /grid/1
mount /dev/vdb1 /grid/1
bash -c "echo '/dev/vdb1        /grid/1         ext4    acl,rw,noatime,nodelalloc       0       2' >> /etc/fstab"

echo "fk-auto-zoo fk-auto-zoo/bucket_name string fk-connekt-zk" | sudo -E debconf-set-selections
sudo apt-get install --yes --allow-unauthenticated fk-auto-zoo
sudo svc -d /etc/service/fk-ops-hosts-populator
sudo svc -u /etc/service/fk-ops-hosts-populator

#Log in to the hosts:
#update the bucket and restart