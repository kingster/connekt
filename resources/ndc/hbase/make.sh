#!/usr/bin/env bash

kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId  fk-connekt-hbase-nn --type i1.large --users /home/kinshuk.bairagi/users.txt --script

kloud-cli --endpoint 10.33.65.0   instance describe --appId fk-connekt-hbase-nn --instanceId 210496

#kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance destroy --appId fk-connekt-hbase-nn --instanceId 259750

kloud-cli --endpoint 10.33.65.0   instance list --appId fk-connekt-hbase-nn
