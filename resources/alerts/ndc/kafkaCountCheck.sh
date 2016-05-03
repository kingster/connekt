#!/bin/bash
msgCount=`curl http://10.32.221.147:9000/clusters/ConnektNDCProd/consumers/ckt/topic/push_9cd14ba3079c6ca91ed3d789125777c2 2>&1| awk  '/Total Lag/{getline; print}'| tr -d '  </>' | sed -e 's/td//g'`
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
