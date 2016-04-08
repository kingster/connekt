#!/usr/bin/env bash

#setup infra-cli and default srcs.list
echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli-svc.list

apt-get update
apt-get install --yes --allow-unauthenticated infra-cli

reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-pf-azkaban --appkey fk-pf-azkaban > /etc/apt/sources.list.d/fk-azkaban.list
apt-get update

#export DEBIAN_FRONTEND=noninteractive

echo "mysql-server-5.5 mysql-server/root_password password root" | sudo debconf-set-selections
echo "mysql-server-5.5 mysql-server/root_password_again password root" | sudo debconf-set-selections

apt-get install --yes --allow-unauthenticated  mysql-server-5.5 mysql-client-5.5

#/etc/init.d/mysql restart
apt-get install --yes --allow-unauthenticated  oracle-j2sdk1.8

echo "fk-w3-azkaban azkaban/config_bucket string fk-connekt-azkaban" | sudo debconf-set-selections
echo "fk-w3-azkaban azkaban/skip_mysql boolean false" | sudo debconf-set-selections

apt-get install --yes --allow-unauthenticated fk-w3-azkaban


echo "n
p
1


w" | /sbin/fdisk /dev/vdb
mkfs.ext4 /dev/vdb1
mkdir -p /storage/1
mount /dev/vdb1 /storage/1
bash -c "echo '/dev/vdb1        /storage/1        ext4        acl,rw,noatime,nodelalloc        0        2' >> /etc/fstab"

mkdir -p /storage/1/azkaban/executions
rm -rf /usr/share/fk-w3-azkaban/executions
ln -s  /storage/1/azkaban/executions /usr/share/fk-w3-azkaban/executions
chmod 775 /usr/share/fk-w3-azkaban/executions

mkdir -p /storage/1/azkaban/jobs
rm -rf /usr/share/fk-w3-azkaban/jobs
ln -s  /storage/1/azkaban/jobs /usr/share/fk-w3-azkaban/jobs
chmod 775 /usr/share/fk-w3-azkaban/jobs

bash -c "echo 'connekt-azkaban' > /etc/default/cosmos-service"
