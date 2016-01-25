package com.flipkart.connekt.commons.dal.impl.couchbase;

/**
 * Created by kinshuk.bairagi on 22/01/16.
 */

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.AsyncQueryResult;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryPlan;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.view.AsyncSpatialViewResult;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.ViewQuery;
import com.google.common.annotations.VisibleForTesting;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;


@VisibleForTesting
public class CouchbaseMockAsyncBucket implements AsyncBucket {

  /* Data store to be used for storing objects when called from Unit Tests */
  private final Map<String, Document<?>> dataStore;

  private final String bucket;
  private final ClusterFacade core;
  private final CouchbaseEnvironment environment;

  public CouchbaseMockAsyncBucket(final ClusterFacade core, final CouchbaseEnvironment environment, final String name) {
    bucket = name;
    this.core = core;
    this.environment = environment;
    dataStore = new HashMap<>();
  }


  @Override
  public String name() {
    return this.bucket;
  }

  @Override
  public Observable<ClusterFacade> core() {
    return Observable.just(core);
  }

  @Override
  public Observable<JsonDocument> get(String id) {
    return get(JsonDocument.create(id));
  }

  @Override
  @SuppressWarnings(value = "unchecked")
  public <D extends Document<?>> Observable<D> get(D document) {
    D result = (D)dataStore.get(document.id());
    if(result != null) {
      return Observable.just(result);
    }
    return Observable.empty();
  }

  @Override
  public <D extends Document<?>> Observable<D> get(String id, Class<D> target) {
    return null;
  }

  @Override
  public Observable<JsonDocument> getFromReplica(String id, ReplicaMode type) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> getFromReplica(D document, ReplicaMode type) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> getFromReplica(String id, ReplicaMode type, Class<D> target) {
    return null;
  }

  @Override
  public Observable<JsonDocument> getAndLock(String id, int lockTime) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> getAndLock(D document, int lockTime) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> getAndLock(String id, int lockTime, Class<D> target) {
    return null;
  }

  @Override
  public Observable<JsonDocument> getAndTouch(String id, int expiry) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> getAndTouch(D document) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> getAndTouch(String id, int expiry, Class<D> target) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> insert(D document) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> insert(D document, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> upsert(D document) {
    this.dataStore.put(document.id(),document);
    return Observable.just(document);
  }

  @Override
  public <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> upsert(D document, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> replace(D document) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> replace(D document, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(D document) {
    this.dataStore.remove(document.id());
    return Observable.just(document);
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(D document, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public Observable<JsonDocument> remove(String id) {
    return null;
  }

  @Override
  public Observable<JsonDocument> remove(String id, PersistTo persistTo, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public Observable<JsonDocument> remove(String id, PersistTo persistTo) {
    return null;
  }

  @Override
  public Observable<JsonDocument> remove(String id, ReplicateTo replicateTo) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(String id, Class<D> target) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, Class<D> target) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> remove(String id, ReplicateTo replicateTo, Class<D> target) {
    return null;
  }

  @Override
  public Observable<AsyncViewResult> query(ViewQuery query) {
    return null;
  }

  @Override
  public Observable<AsyncSpatialViewResult> query(SpatialViewQuery query) {
    return null;
  }

  @Override
  public Observable<AsyncQueryResult> query(Statement statement) {
    return null;
  }

  @Override
  public Observable<AsyncQueryResult> query(Query query) {
    return null;
  }

  @Override
  public Observable<QueryPlan> prepare(Statement statement) {
    return null;
  }

  @Override
  public Observable<QueryPlan> prepare(String statement) {
    return null;
  }

  @Override
  public Observable<Boolean> unlock(String id, long cas) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<Boolean> unlock(D document) {
    return null;
  }

  @Override
  public Observable<Boolean> touch(String id, int expiry) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<Boolean> touch(D document) {
    return null;
  }

  @Override
  public Observable<JsonLongDocument> counter(String id, long delta) {
    return null;
  }

  @Override
  public Observable<JsonLongDocument> counter(String id, long delta, long initial) {
    return null;
  }

  @Override
  public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> append(D document) {
    return null;
  }

  @Override
  public <D extends Document<?>> Observable<D> prepend(D document) {
    return null;
  }

  @Override
  public Observable<AsyncBucketManager> bucketManager() {
    return null;
  }

  @Override
  public Observable<Boolean> close() {
    return null;
  }
}