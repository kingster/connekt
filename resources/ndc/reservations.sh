#!/usr/bin/env bash

###Creating all appId's

kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt --desc="Connekt" --email=connekt-dev@flipkart.com

kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt-hbase-dn --desc="Connekt Hbase DD" --email=connekt-dev@flipkart.com
kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt-hbase-nn --desc="Connekt Hbase NN" --email=connekt-dev@flipkart.com

kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt-kafka --desc="Connekt Kafka" --email=connekt-dev@flipkart.com
kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt-zk --desc="Connekt ZK" --email=connekt-dev@flipkart.com

kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt-mysql --desc="Connekt MySQL" --email=connekt-dev@flipkart.com

kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt-couchbase --desc="Connekt Couchbase" --email=connekt-dev@flipkart.com

kloud-cli --endpoint 10.33.65.0  app create --appId=fk-connekt-azkaban --desc="Connekt Azkaban" --email=connekt-dev@flipkart.com

###Reserving all boxes


#hbase
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt-hbase-nn create --type=i1.large --size=3
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt-hbase-dn create --type=d1.xlarge --size=15

#kafka
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt-kafka create --type=d1.xlarge --size=15

#zk
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt-zk create --type=i1.medium --size=5

#mysql
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt-mysql create --type=i1.xlarge --size=3

#couchbase
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt-couchbase create --type=m1.xlarge --size=6

#azkaban
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt-azkaban create --type=i1.medium --size=2

#app boxes
kloud-cli --endpoint 10.33.65.0 reservation --appId=fk-connekt create --type=c1.xlarge --size=100

#fk-connekt* credentials
Username: connekt-dev
export MEGH_API_PASSWORD="c01^1^3\!<7"



