Couchbase
================

Credentials
-----------
Username : admin
Password : connekt@123

Cluster
-----------
Master: 10.32.161.203

Admin : http://10.32.161.203:8091/index.html

NM
-----------
Master : connekt-couchbase-gretel-0001.nm.flipkart.com

Admin: http://connekt-couchbase-gretel-0001.nm.flipkart.com:8091/index.html

----------
Index the coucbase using following cURL

curl -X POST -H "Cache-Control: no-cache" -H "Postman-Token: 492159d6-ad4e-2d0c-6be7-aee141f8e18c" 
"http://<HOSTNAME>:8093/query?statement=CREATE PRIMARY INDEX ON StatsReporting"

Secondary index on keys:
curl -X POST -H "Cache-Control: no-cache" -H "Postman-Token: 492159d6-ad4e-2d0c-6be7-aee141f8e18c" "http://<HOSTNAME>:8093/query?statement=CREATE INDEX idx ON StatsReporting (META().id) USING GSI"
