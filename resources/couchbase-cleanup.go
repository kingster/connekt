package main

import (
  "reflect"
  "fmt"
  "net/url"
  "io/ioutil"
  "net/http"
  "encoding/json"
  "gopkg.in/couchbaselabs/gocb.v1"
  "flag"
)

func main() {

  hostname := flag.String("host", "localhost", "couchbase cluster host") 
  size  := flag.Int("size", 1234, "size of chunk")
  prefix := flag.String("prefix", "", "prefix")

  flag.Parse()

  fmt.Println("Using Prefix:", *prefix, "with Chunksize of", *size)

  if len(*prefix) > 0 {

    resp, err := http.PostForm("http://" + *hostname + ":8093/query/service", url.Values{"statement": {"SELECT META(p).id FROM StatsReporting p WHERE META(p).id LIKE '" + *prefix + "%'"} })
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

    fmt.Println("Response Count", len(response["results"].([]interface{})))

    cluster, err := gocb.Connect("couchbase://" + *hostname + "")
    if err != nil {
      panic(err)
    }

    bucket, err := cluster.OpenBucket("StatsReporting", "")
    if err != nil {
    	panic(err)
    }

    var chunks = slicer(response["results"].([]interface{}), *size)

    for _, stage := range chunks {
      var items []gocb.BulkOp
      for _, element := range stage {
        id := element.(map[string]interface{})["id"].(string)
        fmt.Println("Deleteing Id... ", id)
        items = append(items, &gocb.RemoveOp{Key: id})
      }

      err = bucket.Do(items)
      if err != nil {
        fmt.Println("Do error:", err)
        panic(err)
      }

    }

  } else {
    fmt.Println("Prefix Cannot Be empty")
  }
}

func slicer(a interface{}, b int) [][]interface{} {
  val := reflect.ValueOf(a)

  origLen := val.Len()
  outerLen := origLen / b
  if origLen % b > 0 {
    outerLen++
  }

  // Make the output slices with all the correct lengths
  c := make([][]interface{}, outerLen)

  for i := range c {
    newLen := b
    if origLen - (i * b + newLen) < 0 {
      newLen = origLen % b
    }
    c[i] = make([]interface{}, newLen)
    for j := range c[i] {
      c[i][j] = val.Index(i * b + j).Interface()
    }
  }

  return c
}

