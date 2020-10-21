package com.iri.backend.tools.service;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;

public interface IMongoDao {

    MongoDatabase db();

    MongoClient client();

    MongoCollection<Document> collectionStore();

    MongoCollection<Document> collectionLock();

    String collectionLockName();

    String collectionStoreName();

}
