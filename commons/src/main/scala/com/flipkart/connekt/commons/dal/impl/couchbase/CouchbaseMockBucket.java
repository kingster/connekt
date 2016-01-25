package com.flipkart.connekt.commons.dal.impl.couchbase;

/**
 * Created by kinshuk.bairagi on 22/01/16.
 */

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.*;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryPlan;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.util.Blocking;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.SpatialViewResult;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.TimeUnit;

@VisibleForTesting
public class CouchbaseMockBucket implements Bucket {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncBucket asyncBucket;
    private final long kvTimeout;
    private final String name;
    private final ClusterFacade core;

    public CouchbaseMockBucket( CouchbaseEnvironment env,  ClusterFacade core,  String name) {
        this.asyncBucket = new CouchbaseMockAsyncBucket(core, env, name);
        this.kvTimeout = env.kvTimeout();
        this.name = name;
        this.core = core;
    }

    @Override
    public AsyncBucket async() {
        return asyncBucket;
    }

    @Override
    public ClusterFacade core() {
        return this.core;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public JsonDocument get(String id) {
        return get(JsonDocument.create(id));
    }

    @Override
    public JsonDocument get(String id, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D get(D document) {
        return Blocking.blockForSingle(async().get(document).singleOrDefault(null), kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D get(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type) {
        return null;
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type) {
        return null;
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime) {
        return null;
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry) {
        return null;
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D insert(D document) {
        return Blocking.blockForSingle(async().upsert(document).singleOrDefault(null), kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D upsert(D document) {
        return Blocking.blockForSingle(asyncBucket.upsert(document).single(), kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonDocument remove(String id) {
        return null;
    }

    @Override
    public JsonDocument remove(String id, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo) {
        return null;
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonDocument remove(String id, ReplicateTo replicateTo) {
        return null;
    }

    @Override
    public JsonDocument remove(String id, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target) {
        return null;
    }

    @Override
    public <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public ViewResult query(ViewQuery query) {
        return null;
    }

    @Override
    public SpatialViewResult query(SpatialViewQuery query) {
        return null;
    }

    @Override
    public ViewResult query(ViewQuery query, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public SpatialViewResult query(SpatialViewQuery query, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public QueryResult query(Statement statement) {
        return null;
    }

    @Override
    public QueryResult query(Statement statement, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public QueryResult query(Query query) {
        return null;
    }

    @Override
    public QueryResult query(Query query, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public QueryPlan prepare(String statement) {
        return null;
    }

    @Override
    public QueryPlan prepare(Statement statement) {
        return null;
    }

    @Override
    public QueryPlan prepare(String statement, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public QueryPlan prepare(Statement statement, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public Boolean unlock(String id, long cas) {
        return null;
    }

    @Override
    public Boolean unlock(String id, long cas, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> Boolean unlock(D document) {
        return null;
    }

    @Override
    public <D extends Document<?>> Boolean unlock(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public Boolean touch(String id, int expiry) {
        return null;
    }

    @Override
    public Boolean touch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> Boolean touch(D document) {
        return null;
    }

    @Override
    public <D extends Document<?>> Boolean touch(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonLongDocument counter(String id, long delta) {
        return null;
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial) {
        return null;
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry) {
        return null;
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D append(D document) {
        return null;
    }

    @Override
    public <D extends Document<?>> D append(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public <D extends Document<?>> D prepend(D document) {
        return null;
    }

    @Override
    public <D extends Document<?>> D prepend(D document, long timeout, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public BucketManager bucketManager() {
        return null;
    }

    @Override
    public Boolean close() {
        return null;
    }

    @Override
    public Boolean close(long timeout, TimeUnit timeUnit) {
        return null;
    }
}
