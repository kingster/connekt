#!/bin/bash -

set -x
set -o nounset    # Treat unset variables as an error

# Setup repositories for IaaS and debian backports.
echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli-svc.list

echo "deb http://10.47.2.22:80/repos/fk-config-service-confd/35 /" > /etc/apt/sources.list.d/fdp-infra-base.list
echo "deb http://10.47.2.22:80/repos/fdp-infra-hdp-repos/9 /" >> /etc/apt/sources.list.d/fdp-infra-base.list

# Install auth client package so that it doesn't conflict with other packages later
apt-get install fk-ops-auth-client --yes --allow-unauthenticated

# Remove /etc/hosts entry for IaaS provided hostname
# source /etc/default/megh/env_var
#/bin/sed -i "/${MEGH_HOSTNAME}/s/^/#/" /etc/hosts

env_bucket_name=fk-connekt-ambari

# Add IP mappings for all services used.
mirror_ip="10.65.208.107"
if grep -q "^${mirror_ip}" /etc/hosts; then
	echo "Mirror IP is already set. Skipping."
else
	echo "10.65.208.107 fdp-infra-mirror hnode5.nm.flipkart.com" >> /etc/hosts
fi

# Install reposervice CLI
apt-get update
apt-get install --yes --allow-unauthenticated infra-cli

apt-get install fdp-infra-hdp-repos --yes --allow-unauthenticated

# Wait indefinitely for repos to be fetched from Config Service
sleep 10

sl=10
counter=0
while true; do
	repos=`cat /etc/apt/sources.list.d/fdp-infra-hdp-repos.list | wc -l`
	if [ ${repos} -lt 3 ]; then
		echo "Number of repos fetched: ${repos}"
        let "wait = ${counter} * ${sl}"
        echo "Waiting for Repos to be fetched from Config Service. Total Wait in Seconds: ${wait}"
		sleep ${sl}
	else
		break
	fi
    let counter++
done

apt-get update

# Configure and install cluster environment package
echo "fdp-infra-cluster-env cluster-env/bucketid string ${env_bucket_name}" | sudo -E debconf-set-selections
apt-get install --yes --allow-unauthenticated fdp-infra-cluster-env

#Swith back to original host-populator
mkdir -p /usr/local/fk-ops-hosts-populator/buckets/
echo "fk-connekt-hdfs-hosts/etc-hosts" > /usr/local/fk-ops-hosts-populator/buckets/fk-connekt-hdfs.conf
sudo apt-get install --yes --allow-unauthenticated fk-ops-hosts-populator


# Configure libc6 flag for uninterrupted install
echo "libc6 libraries/restart-without-asking boolean true" | sudo -E debconf-set-selections

# Install HDP setup helper scripts.
apt-get install --yes --allow-unauthenticated fdp-infra-hdp-scripts
