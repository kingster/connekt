#!/usr/bin/env bash

kloud-cli --zone in-mumbai-gateway --user connekt-dev  instance create --appId fk-connekt-smtp --type c0.xlarge.e1 --users /home/kinshuk.bairagi/users.txt --script /home/kinshuk.bairagi/setup.sh --tag bcc

#kloud-cli --zone in-mumbai-gateway --user connekt-dev  instance destroy --appId fk-connekt-smtp --instanceId 393258

#kloud-cli --zone in-mumbai-gateway  --user connekt-dev instance describe --appId fk-connekt-smtp --instanceId 393258

kloud-cli --zone in-mumbai-gateway  instance list  --appId fk-connekt-smtp
