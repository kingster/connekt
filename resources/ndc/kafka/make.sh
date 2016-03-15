#!/usr/bin/env bash

SETUP_TEMPLATE=/home/kinshuk.bairagi/kafka/setup.tmpl.sh
SETUP=/home/kinshuk.bairagi/kafka/setup.sh

for i in `seq 1 12`;  do
    sed -e "s/__BROKER_ID__/$i/g" "$SETUP_TEMPLATE" >  "$SETUP"
    kloud-cli --endpoint ga --user connekt-dev instance create --appId fk-connekt-kafka --type d1.xlarge --users /home/kinshuk.bairagi/users.txt --script "$SETUP" --tag thor
done

#kloud-cli --endpoint ga --user connekt-dev  instance describe --appId fk-connekt-kafka --instanceId 210496
#kloud-cli --endpoint ga --user connekt-dev  instance list --appId fk-connekt-kafka
#kloud-cli --endpoint ga --user connekt-dev instance destroy --appId fk-connekt-kafka --instanceId 367647

#kill
#kloud-cli --endpoint ga  --user connekt-dev  instance list  --appId fk-connekt-kafka | grep -A 4 cluster1 | grep id | tr -td ":\" id" | xargs -I {} kloud-cli --endpoint ga --user connekt-dev  instance destroy --appId fk-connekt-kafka --instanceId {}

