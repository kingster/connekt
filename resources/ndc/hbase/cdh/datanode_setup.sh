#!/usr/bin/env bash
#For Data node
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli.list
apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated infra-cli
reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-ckt-hbase-hdfs --appkey fk-ckt-hbase-dn-ga > /etc/apt/sources.list.d/fk-bro-hbase-zk.list


apt-get update --allow-unauthenticated
apt-get install --yes --allow-unauthenticated fk-cdh-repo
apt-get update --allow-unauthenticated

reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-connekt-commons --appkey connekt > /etc/apt/sources.list.d/fk-connekt-commons.list

sudo apt-get update

echo 'team_name="Connekt"' > /etc/default/nsca_wrapper
echo 'nagios_server_ip="10.47.2.198"' >> /etc/default/nsca_wrapper

sudo apt-get install --yes --allow-unauthenticated fk-nagios-common --reinstall


echo "fk-auto-zoo fk-auto-zoo/bucket_name string fk-ckt-hbase-zk" | sudo -E debconf-set-selections
echo "fk-hdfs-config hdfs/bucketid string fk-ckt-hdfs" | sudo -E debconf-set-selections
echo "fk-hbase-config hbase/bucketid string  fk-ckt-hbase" | sudo -E debconf-set-selections

apt-get install --yes --allow-unauthenticated fk-hdfs-builder
apt-get install --yes --allow-unauthenticated fk-hbase-builder


apt-get update --allow-unauthenticated
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


bash -c "echo 'session required  pam_limits.so' >> /etc/pam.d/common-session"

sudo /usr/lib/fk-hdfs-builder/bin/install-service.sh -s datanode
sudo /usr/lib/fk-hbase-builder/bin/install-service.sh -s regionserver

#Command(first update config bucket then run these command)
#sudo /etc/init.d/hadoop-hdfs-datanode restart
#sudo /etc/init.d/hbase-regionserver restart
