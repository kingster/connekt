#For JornalNode + zookeeper + :
#!/usr/bin/env bash
#!/bin/bash
echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli.list
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated infra-cli
reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-ckt-hbase-hdfs --appkey fk-ckt-hbase-dn-ga > /etc/apt/sources.list.d/fk-ckt-hbase-nn.list
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated fk-cdh-repo
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated python

reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-connekt-commons --appkey connekt > /etc/apt/sources.list.d/fk-connekt-commons.list

sudo apt-get update

echo 'team_name="Connekt"' > /etc/default/nsca_wrapper
echo 'nagios_server_ip="10.47.2.198"' >> /etc/default/nsca_wrapper

sudo apt-get install --yes --allow-unauthenticated fk-nagios-common --reinstall


echo "fk-auto-zoo fk-auto-zoo/bucket_name string fk-ckt-hbase-zk" | sudo -E debconf-set-selections
echo "fk-hdfs-config hdfs/bucketid string fk-ckt-hdfs" | sudo -E debconf-set-selections
echo "fk-hbase-config hbase/bucketid string  fk-ckt-hbase" | sudo -E debconf-set-selections

echo "n
p
1


w" | fdisk /dev/vdb
mkfs.ext4 /dev/vdb1
mkdir -p /grid/1
mount /dev/vdb1 /grid/1
bash -c "echo '/dev/vdb1         /grid/1         ext4            acl,rw,noatime,nodelalloc       0       2' >> /etc/fstab"

bash -c "echo 'session required  pam_limits.so' >> /etc/pam.d/common-session"
apt-get install --yes --allow-unauthenticated fk-auto-zoo
svc -d /etc/service/fk-ops-hosts-populator
svc -u /etc/service/fk-ops-hosts-populator
apt-get install --yes --allow-unauthenticated fk-hdfs-builder
apt-get install --yes --allow-unauthenticated  fk-hbase-builder

#(first update config bucket then run these command)
#Commands:
#sudo /etc/init.d/fk-zookeeper-server restart
#sudo /usr/lib/fk-hdfs-builder/bin/install-service.sh -s journalnode(On Journalnode)
#sudo /usr/lib/fk-hdfs-builder/bin/install-service.sh -s namenode(On namenode)
#sudo /etc/init.d/hadoop-hdfs-journalnode restart(On Journalnode)
#sudo -u hdfs hdfs zkfc -formatZK <-- Only on one namenode
#sudo /etc/init.d/hadoop-hdfs-zkfc start <-- On both namenodes
#sudo -u hdfs hdfs namenode -format <-- Only on primary namenode
#sudo /etc/init.d/hadoop-hdfs-namenode restart <-- Only on primary namenode
#sudo -u hdfs hdfs namenode -bootstrapStandby <-- Only on standby namenode
#sudo /etc/init.d/hadoop-hdfs-namenode restart <-- Only on standby namenode
#sudo /usr/lib/fk-hbase-builder/bin/install-service.sh -s master <-- Only on hbase master
#sudo /etc/init.d/hbase-master restart <-- Only on hbase master




