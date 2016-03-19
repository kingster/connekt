#!/usr/bin/env bash

kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId  fk-connekt-hbase --type d1.xlarge --users /home/kinshuk.bairagi/users.txt --script /home/aman.shrivastava/ambari/setup/setup.sh --tag dn-firestorm
kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId  fk-connekt-hbase --type d1.medium --users /home/kinshuk.bairagi/users.txt --script /home/aman.shrivastava/ambari/setup/setup.sh --tag collector-firestorm
kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId  fk-connekt-hbase --type i1.large --users /home/kinshuk.bairagi/users.txt --script /home/aman.shrivastava/ambari/setup/setup.sh --tag jn-firestorm
kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId  fk-connekt-hbase --type c1.medium --users /home/kinshuk.bairagi/users.txt --script /home/aman.shrivastava/ambari/setup/setup.sh --tag ambari-firestorm
kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId  fk-connekt-hbase --type i1.xlarge --users /home/kinshuk.bairagi/users.txt --script /home/aman.shrivastava/ambari/setup/setup.sh --tag nn-firestorm

kloud-cli --endpoint 10.33.65.0   instance describe --appId fk-connekt-hbase --instanceId 397061

#kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance destroy --appId fk-connekt-hbase --instanceId 397067

kloud-cli --endpoint 10.33.65.0   instance list --appId fk-connekt-hbase


#kill
#kloud-cli --endpoint ga  --user connekt-dev  instance list  --appId fk-connekt-hbase | grep id | tr -td ":\" id" | xargs -I {} kloud-cli --endpoint ga --user connekt-dev  instance destroy --appId fk-connekt-hbase --instanceId {}

