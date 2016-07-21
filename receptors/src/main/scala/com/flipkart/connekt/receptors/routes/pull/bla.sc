
import com.couchbase.client.java.document.json.JsonObject
import com.google.gson.{JsonElement, JsonParser}
import scala.collection.convert._


import sys.process._

var key = ""
val prefixCurl = Seq("curl", "-v" ,"http://ce-sandbox-cb-0001.nm.flipkart.com:8093/query/service", "--data-urlencode", "statement=SELECT META(p).id FROM StatsReporting p WHERE META(p).id LIKE '20160708.connekt-insomnia.Automation.a%'")
val prefix = prefixCurl !!
val parser = new JsonParser()
val o = parser.parse(prefix).getAsJsonObject()
val results = o.get("results")
println("\nSIZE " + results.getAsJsonArray.size())
val it = results.getAsJsonArray.iterator()
while(it.hasNext){
  key = it.next().getAsJsonObject.get("id").toString
  val deleteCurl = Seq("curl", "-v" ,"http://ce-sandbox-cb-0001.nm.flipkart.com:8093/query/service",  s"DELETE FROM StatsReporting p USE KEYS $key RETURNING p")
  val deleteResult = deleteCurl !!

  println("\nResponse" + deleteResult)
}
