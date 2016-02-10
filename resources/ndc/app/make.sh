 kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId fk-connekt --type c1.xlarge --users /home/kinshuk.bairagi/users.txt --script /home/kinshuk.bairagi/connekt/setup.sh --tag prod

 #kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance destroy --appId fk-connekt --instanceId 254093

 #kloud-cli --endpoint 10.33.65.0  --user connekt-dev instance describe --appId fk-connekt --instanceId 254093

 kloud-cli --endpoint 10.33.65.0   instance list  --appId fk-connekt