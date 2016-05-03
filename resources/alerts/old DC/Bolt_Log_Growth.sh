#!/bin/bash
bolt_host_num=(01 02 03 04 05 06 07 08 09)

HOME='/var/log/azkabanScripts'
tempBolt='tempConnnektBoltLog'
logPath='/var/log/flipkart/pf/fk-pf-connekt'
bolt_parameters=("$logPath/busybees-processors.log" 'ERROR' 4000 6000 "$logPath/busybees-dao.log" 'ERROR' 4000 6000 "$logPath/busybees-service.log" 'ERROR' 4000 6000 "$logPath/busybees-default.log" 'ERROR' 4000 6000 "$logPath/busybees-factory.log" 'ERROR' 4000 6000 "$logPath/busybees-clients.log" 'ERROR' 4000 6000 "$logPath/catalina.log" 'ERROR' 4000 6000)

alerts_to_supress=''
status=true

echo "" > $tempBolt
echo "************* Checking Bolts' LOG GROWTH ***************" >> $tempBolt

for i in ${bolt_host_num[@]}
do
    host="fk-connekt-bolt-00$i.nm.flipkart.com"
	echo "*************** Host: $host ***************" >> $tempBolt
	for (( j=0; j<${#bolt_parameters[@]}; j=j+4 ))
	do
		command="$HOME/log_growth.php -l ${bolt_parameters[j]} -c ${bolt_parameters[j+3]} -w ${bolt_parameters[j+2]} -p ${bolt_parameters[j+1]} -d 5 -a $alerts_to_supress"
		ssh -i /usr/share/fk-w3-azkaban/conf/azkaban_rsa -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no fk-azkaban-remote@"$host" "$command" 2>/dev/null >> $tempBolt
	done
echo -e "*************************************************************\n" >> $tempBolt
done

bolt_logs=`cat $tempBolt`

bolt_uniq_errors=`echo "$bolt_logs"|grep -cv -e "LOG GROWTH" -e "Error check starting point" -e "Host:" -e "\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*" -e "^$"`


if [ "$bolt_uniq_errors" -ne 0 ]
then
	echo "$bolt_logs"
    status=false
fi


if [ "$status" == "true" ]
then
	exit 0
else
	exit 1
fi
