package main

import (
  "fmt"
  "net/url"
  "io/ioutil"
  "net/http"
  "encoding/json"
)

func main() {

  hostname := "10.32.189.22"

  resp, err := http.PostForm("http://" + hostname + ":8093/query/service", url.Values{"statement": {"SELECT META(p).id FROM StatsReporting p WHERE META(p).id LIKE '20160730.seller%'"} })
  if err != nil {
    panic(err)
  }
  defer resp.Body.Close()
  body, err := ioutil.ReadAll(resp.Body)
  if err != nil {
    panic(err)
  }

  var response map[string]interface{}
  err = json.Unmarshal(body, &response)
  if err != nil {
    panic(err)
  }

  fmt.Println("Response: ", response["results"])

  for _, element := range response["results"].([]interface{}) {
    id := element.(map[string]interface{})["id"].(string)
    fmt.Println("Deleteing Id... ", id)

    del, _ := http.PostForm("http://" + hostname + ":8093/query/service", url.Values{"statement": {"DELETE FROM StatsReporting p USE KEYS '" + id + "' RETURNING p"} })
    defer del.Body.Close()

    bb, err := ioutil.ReadAll(del.Body)
    if err != nil {
      panic(err)
    }

    fmt.Println("Response: ", string(bb))

  }

}
