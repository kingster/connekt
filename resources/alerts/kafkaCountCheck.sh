#!/bin/bash
msgCount=`curl http://fk-bro-kafka-ga-preprod4-266223:9000/clusters/ConnektNDCProd/consumers/ckt/topic/push_9cd14ba3079c6ca91ed3d789125777c2 2>&1| awk  '/Total Lag/{getline; print}'| tr -d '  </>' | sed -e 's/td//g'`
echo "Count: $msgCount"

if [ $msgCount -gt 100000 ]
  then
     echo "CRITICAL: QueueSize: $msgCount"
     echo "Job execution failed"
     exit 1
  else
    echo "Job executed successfully"
    exit 0
fi
