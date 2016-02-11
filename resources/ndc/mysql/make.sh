kloud-cli --endpoint 10.33.65.0 --user connekt-dev  instance create --appId fk-connekt-mysql --type i1.xlarge --users /home/kinshuk.bairagi/users.txt --script /home/kinshuk.bairagi/mysql/setup.sh --tag kanchenjunga

 kloud-cli --endpoint 10.33.65.0   instance list  --appId fk-connekt-mysql

#kloud-cli --endpoint 10.33.65.0   instance describe --appId fk-bro-mysql-ga --instanceId <instanceId>
#kloud-cli --endpoint 10.33.65.0   instance destroy --appId fk-bro-mysql-ga --instanceId <instanceId>


#1) goto master and run "create user 'replication'@'10.%' IDENTIFIED BY 'dessert'; "
#2) in master: GRANT ALL PRIVILEGES ON *.* TO 'replication'@'10.%' IDENTIFIED BY PASSWORD '*B8629906DF56094C1644CCC18AAE0DF60B5B781F' WITH GRANT OPTION
#3) in master: show master status;  -- note the filename and position
#4) in slave: CHANGE MASTER TO MASTER_HOST='MASTER-IP-ADDRESS', MASTER_USER='replication', MASTER_PASSWORD='dessert', MASTER_LOG_FILE ='FRom show master status from above’, MASTER_LOG_POS=<postion>;
