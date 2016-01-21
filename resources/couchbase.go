package main

import (
	"github.com/couchbase/go-couchbase"
	"log"
)

func mf(err error, msg string) {
	if err != nil {
		log.Fatalf("%v: %v", msg, err)
	}
}

func main() {

	c, err := couchbase.Connect("http://admin:kingster@10.32.113.29:8091/")
	mf(err, "connect - http://admin:kingster@10.32.113.29:8091/")

	p, err := c.GetPool("default")
	mf(err, "pool")

	b, err := p.GetBucket("default")
	mf(err, "bucket")

	err = b.Set("dammkey", 90, map[string]interface{}{"x": 1})
	mf(err, "set")

	ob := map[string]interface{}{}
	err = b.Get("dammkey", &ob)
	mf(err, "get")

}
