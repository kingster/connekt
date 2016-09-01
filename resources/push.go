package main

import (
	"bufio"
	"fmt"
	"os"
	"strings"
	"net/http"
	"bytes"
	"io/ioutil"
	"flag"
)

var f_main *os.File

func main() {

	file := flag.String("file", "no-value", "FileName")
	contextId := flag.String("contextId", "SILENT", "ContextId (Default SILENT)")

	flag.Parse()

	fmt.Printf("Reading File : %s\n", *file)

	url := "http://10.33.157.237:28000/v1/send/push/unknown/RetailApp"

	accIds, err := readLines(*file)
	if err != nil {
	    fmt.Printf("readLines Error: %s", err)
	}

	for _, deviceId := range accIds {

		deviceIdTrim := strings.TrimSpace(deviceId)

		var jsonStr = []byte(`{
      "channel": "PN",
      "sla": "H",
      "contextId" : "` + *contextId + `",
      "channelData": {
        "type": "PN",
        "data": {
                "id": "`+ *contextId +`",
                "type" : 6
        }
      },
      "channelInfo" : {
          "type" : "PN",
          "delayWhileIdle": false,
          "deviceIds": ["` + deviceIdTrim + `"]
      },
      "meta": {}
    }`)

		req, err := http.NewRequest("POST", url , bytes.NewBuffer(jsonStr))
		req.Header.Set("x-api-key", "connekt-genesis")
		req.Header.Set("Content-Type", "application/json")

		client := &http.Client{}
		resp, err := client.Do(req)
		if err != nil {
			panic(err)
		}
		defer resp.Body.Close()

    _, err = ioutil.ReadAll(resp.Body)
     if err != nil {
         panic(err)
    }

		fmt.Println("Device:", deviceIdTrim, "| Response Status:", resp.Status)

	}
}


// readLines reads a whole file into memory
// and returns a slice of its lines.
func readLines(path string) ([]string, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	var lines []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		lines = append(lines, scanner.Text())
	}
	return lines, scanner.Err()
}
