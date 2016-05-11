#!/bin/bash
bolt_host_num=(01 02 03 04 05 06 07 08 09)

HOME='/usr/share/fk-azkaban-remote-job/scripts'
tempBolt='tempConnnektBoltShutdownLog'
tempCheckErrorCount='tempConnektErrorCount'
logPath='/var/log/flipkart/pf/fk-pf-connekt'
bolt_parameters=("$logPath/busybees-processors.log" "$logPath/catalina.log" )
pattern=('StageSupervision Handle Unknown Exception')
alerts_to_supress=''
status=true

echo "" > $tempBolt
echo "************* Checking Bolts' Topology Shutdown Error ***************" >> $tempBolt

for i in ${bolt_host_num[@]}
do
  host="fk-connekt-bolt-00$i.nm.flipkart.com"
	echo "*************** Host: $host ***************" >> $tempBolt
	for (( j=0; j<${#bolt_parameters[@]}; j=j+1 ))
      do
          command="bash $HOME/checkError.sh ${bolt_parameters[$j]} \"${pattern[0]}\" 30 $alert_to_suppress"
          ssh -i /usr/share/fk-w3-azkaban/conf/azkaban_rsa -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no fk-azkaban-remote@"$host" "$command" 2>/dev/null >> $tempBolt > $tempCheckErrorCount
  done
  countError=`grep -c "${pattern[0]}" "$tempCheckErrorCount"`
  if [ "$countError" -gt 0 ]
  then
    echo "StageSupervision Handle Unknown Exception in host : $host"
    status=false
  fi
  echo -e "**************************************************************\n" >> $tempBolt
done

if [ "$status" == "false" ]
then
  echo "Job execution failed"
  exit 1
else
  echo "Job executed success"
  exit 0
fi
