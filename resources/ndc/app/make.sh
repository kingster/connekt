#!/usr/bin/env bash

kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId fk-connekt --type c1.xlarge --users /home/kinshuk.bairagi/users.txt --script /home/kinshuk.bairagi/connekt/setup.sh --tag prod

#kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance destroy --appId fk-connekt --instanceId 390035

#kloud-cli --endpoint 10.33.65.0  --user connekt-dev instance describe --appId fk-connekt --instanceId 254093

kloud-cli --endpoint 10.33.65.0   instance list  --appId fk-connekt

#### Creating Instance group
## This is for the record only, never run this.
# kloud-cli --endpoint ga instanceTemplate create --appId fk-connekt --name aleph --type c1.xlarge --users  /home/kinshuk.bairagi/users.txt --script  /home/kinshuk.bairagi/app/setup.sh
# kloud-cli --endpoint ga --user connekt-dev instanceGroup create --appId fk-connekt  --name avengers --template aleph --size 10

##Resize API Servers
## Remeber to send in mail to elb team for refresh
#kloud-cli --endpoint 10.33.65.0 instanceGroup resize --appId fk-connekt --name avengers --size 10