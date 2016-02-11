#!/usr/bin/env bash

kloud-cli --endpoint 10.33.65.0   instance create --user connekt-dev --appId fk-connekt-zk --type i1.medium --users /home/kinshuk.bairagi/users.txt --script /home/ankesh.maheshwari/zookeeper/setup.sh

#kloud-cli --endpoint 10.33.65.0   instance describe --appId fk-connekt-zk --instanceId 210496

kloud-cli --endpoint 10.33.65.0   instance destroy --appId fk-connekt-zk --instanceId 210496

#kloud-cli --endpoint 10.33.65.0   instance destroy --appId fk-connekt-zk --instanceId 249914

#kloud-cli --endpoint 10.33.65.0   instance list --appId fk-connekt-zk
