#!/bin/bash
set -x
msgCountWindows=`curl http://10.32.221.147:9000/clusters/ConnektNDCProd/consumers/ckt_windows/topic/push_9cd14ba3079c6ca91ed3d789125777c2 2>&1| awk  '/Total Lag/{getline; print}'| tr -d '  </>' | sed -e 's/td//g'`
msgCountAndroid=`curl http://10.32.221.147:9000/clusters/ConnektNDCProd/consumers/ckt_android/topic/push_9cd14ba3079c6ca91ed3d789125777c2 2>&1| awk  '/Total Lag/{getline; print}'| tr -d '  </>' | sed -e 's/td//g'`
msgCountIos=`curl http://10.32.221.147:9000/clusters/ConnektNDCProd/consumers/ckt_ios/topic/push_9cd14ba3079c6ca91ed3d789125777c2 2>&1| awk  '/Total Lag/{getline; print}'| tr -d '  </>' | sed -e 's/td//g'`
msgCountOpenWeb=`curl http://10.32.221.147:9000/clusters/ConnektNDCProd/consumers/ckt_openweb/topic/push_9cd14ba3079c6ca91ed3d789125777c2 2>&1| awk  '/Total Lag/{getline; print}'| tr -d '  </>' | sed -e 's/td//g'`
failure=0
echo "Count: $msgCount"

if [ $msgCountWindows -gt 100000 ]
  then
     echo "CRITICAL: QueueSize: $msgCountWindows for windows"
     failure=1
fi

if [ $msgCountAndroid -gt 100000 ]
  then
     echo "CRITICAL: QueueSize: $msgCountAndroid for android"
     failure=1
fi

if [ $msgCountIos -gt 100000 ]
  then
     echo "CRITICAL: QueueSize: $msgCountIos for ios"
     failure=1
fi

if [ $msgCountOpenWeb -gt 100000 ]
  then
     echo "CRITICAL: QueueSize: $msgCountOpenWeb for openweb"
     failure=1
fi

if [ $failure -eq 1 ]
  then
    echo "Job execution failed"
    exit 1
  else
    echo "Job executed successfully"
    exit 0
fi
