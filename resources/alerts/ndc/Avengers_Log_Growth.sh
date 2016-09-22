#!/bin/bash
avengers_hosts=$(curl --silent "http://10.33.65.0:8080/compute/v1/apps/fk-connekt/instances?instanceGroup=avengers&view=summary" | jq "map(.primary_ip)" | tr -td '"[] ,' | sed '/^\s*$/d')

HOME='/usr/share/fk-azkaban-remote-job/scripts'
tempAvengers='tempConnektAvengersLog'
logPath='/var/log/flipkart/pf/fk-pf-connekt'
avengers_parameters=("$logPath/receptors-service.log" 'ERROR' 4000 6000 "$logPath/receptors-access.log" 'ERROR' 4000 6000 "$logPath/receptors-dao.log" 'ERROR' 4000 6000 "$logPath/receptors-processor.log" 'ERROR' 4000 6000 "$logPath/receptors-factory.log" 'ERROR' 4000 6000 "$logPath/catalina.log" 'ERROR' 4000 6000)
alerts_to_supress='dummy'
status=true

echo "" > $tempAvengers
echo "************* Checking Recepters' LOG GROWTH ***************" >> $tempAvengers

echo $avengers_hosts | while read i
do
   	echo "*************** Host: $i ***************" >> $tempAvengers
	for (( j=0; j<${#avengers_parameters[@]}; j=j+4 ))
	do
		command="$HOME/log_growth.php -l ${avengers_parameters[j]} -c ${avengers_parameters[j+3]} -w ${avengers_parameters[j+2]} -p ${avengers_parameters[j+1]} -d 5 -a $alerts_to_supress"
		ssh -i /usr/share/fk-w3-azkaban/conf/azkaban_rsa -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no fk-azkaban-remote@"$i" "$command" 2>/dev/null >> $tempAvengers
	done
echo -e "*************************************************************\n" >> $tempAvengers
done

avengers_logs=`cat $tempAvengers`
cat "$tempAvengers"
avengers_uniq_errors=`echo "$avengers_logs"|grep -cv -e "LOG GROWTH" -e "Error check starting point" -e "Host:" -e "\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*" -e "^$"`

if [ "$avengers_uniq_errors" -ne 0 ]
then
	echo "$avengers_logs"
    status=false
fi

rm "$tempAvengers"
if [ "$status" == "true" ]
then
	exit 0
else
	exit 1
fi
