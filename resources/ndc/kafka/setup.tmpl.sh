#!/usr/bin/env bash

#setup infra-cli and default srcs.list
echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli-svc.list
apt-get update
apt-get install --yes --allow-unauthenticated infra-cli
echo "export KAFKA_CONFIG_BUCKET=fk-connekt-kafka" >> /etc/default/fk-3p-kafka.env
echo "export KAFKA_CONFIG_BROKER_ID=__BROKER_ID__" >> /etc/default/fk-3p-kafka.env

#setup your package
reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-bro-kafka --appkey fk-bro-kafka-ga > /etc/apt/sources.list.d/fk-bro-kafka.list
apt-get update

apt-get install --yes --allow-unauthenticated parted

parted --script /dev/vdb -- mklabel gpt mkpart vol ext4 0% 85%
mkfs.ext4 /dev/vdb1
mkdir -p /storage/1
mount /dev/vdb1 /storage/1
bash -c "echo '/dev/vdb1        /storage/1        ext4        acl,rw,noatime,nodelalloc        0        2' >> /etc/fstab"

parted --script /dev/vdc -- mklabel gpt mkpart vol ext4 0% 85%
mkfs.ext4 /dev/vdc1
mkdir -p /storage/2
mount /dev/vdc1 /storage/2
bash -c "echo '/dev/vdc1        /storage/2        ext4        acl,rw,noatime,nodelalloc        0        2' >> /etc/fstab"

parted --script /dev/vdd -- mklabel gpt mkpart vol ext4 0% 85%
mkfs.ext4 /dev/vdd1
mkdir -p /storage/3
mount /dev/vdd1 /storage/3
bash -c "echo '/dev/vdd1        /storage/3        ext4        acl,rw,noatime,nodelalloc        0        2' >> /etc/fstab"

parted --script /dev/vde -- mklabel gpt mkpart vol ext4 0% 85%
mkfs.ext4 /dev/vde1
mkdir -p /storage/4
mount /dev/vde1 /storage/4
bash -c "echo '/dev/vde1        /storage/4        ext4        acl,rw,noatime,nodelalloc        0        2' >> /etc/fstab"


apt-get install --yes --allow-unauthenticated fk-3p-kafka-0.8.2.x

sudo mkdir -p /storage/1/fk-3p-kafka/logs
sudo chown -R fk-3p-kafka:fk-3p /storage/1/fk-3p-kafka

sudo mkdir -p /storage/2/fk-3p-kafka/logs
sudo chown -R fk-3p-kafka:fk-3p /storage/2/fk-3p-kafka

sudo mkdir -p /storage/3/fk-3p-kafka/logs
sudo chown -R fk-3p-kafka:fk-3p /storage/3/fk-3p-kafka

sudo mkdir -p /storage/4/fk-3p-kafka/logs
sudo chown -R fk-3p-kafka:fk-3p /storage/4/fk-3p-kafka
