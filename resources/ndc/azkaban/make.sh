#!/usr/bin/env bash


kloud-cli --endpoint 10.33.65.0  instance --user connekt-dev  create --appId fk-connekt-azkaban --type i1.medium --users /home/kinshuk.bairagi/users.txt --script /home/kinshuk.bairagi/connekt/azkaban/setup.sh --tag prod

kloud-cli --endpoint 10.33.65.0  instance list --appId fk-connekt-azkaban

#kloud-cli --endpoint 10.33.65.0 --user connekt-dev   instance destroy --appId fk-connekt-azkaban --instanceId 389703
