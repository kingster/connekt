
#setup infra-cli and default srcs.list
echo "deb http://wzy-mirror.nm.flipkart.com/ftp.debian.org/debian wheezy-backports main" > /etc/apt/sources.list.d/wzy-backports.list
echo "deb http://10.47.2.22:80:80/repos/infra-cli/3 /" > /etc/apt/sources.list.d/infra-cli-svc.list
apt-get update
apt-get install --yes --allow-unauthenticated infra-cli

reposervice --host repo-svc-app-0001.nm.flipkart.com --port 8080 env --name fk-connekt-couchbase --appkey connekt > /etc/apt/sources.list.d/fk-couchbase.list

sudo apt-get update
# couchbase-server doesn't install libssl0.9.8 automatically
sudo apt-get install --yes --allow-unauthenticated libssl0.9.8
sudo apt-get install --yes --allow-unauthenticated couchbase-server

# couchbase-cli python scripts expect python in /usr/bin
sudo ln -s /usr/bin/python2.7 /usr/bin/python || true

