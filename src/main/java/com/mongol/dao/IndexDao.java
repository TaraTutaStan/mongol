package com.iri.backend.tools.dao;


import com.iri.backend.tools.chengeSet.ChangeEntry;
import com.iri.backend.tools.service.IMongoDao;
import com.mongodb.client.model.IndexOptions;
import io.reactivex.Flowable;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class IndexDao {
    private static final Logger logger = LoggerFactory.getLogger(ChangeEntryDao.class);

    private IMongoDao mongoDao;

    public IndexDao(IMongoDao mongoDao) {
        this.mongoDao = mongoDao;
    }

    public void createRequiredUniqueIndex() {
        Flowable.fromPublisher(mongoDao.collectionStore().createIndex(new Document()
                        .append(ChangeEntry.KEY_CHANGEID, 1)
                        .append(ChangeEntry.KEY_AUTHOR, 1),
                new IndexOptions().unique(true)
        )).firstElement().blockingGet();
    }

    public Document findIndex() {
        final var ns = mongoDao.db().getName() + "." + mongoDao.collectionStoreName();
        final var key = new Document()
                .append(ChangeEntry.KEY_CHANGEID, 1)
                .append(ChangeEntry.KEY_AUTHOR, 1);

        return Flowable.fromPublisher(mongoDao.collectionStore().listIndexes())
                .filter(it -> it.get("key").equals(key) && it.get("ns").equals(ns)).firstElement().blockingGet();

    }

    public boolean isUnique(Document index) {
        return Optional.ofNullable(index.get("unique"))
                .filter(it -> it instanceof Boolean)
                .map(it -> true)
                .orElse(false);
    }

    public void dropIndex(Document index) {
        mongoDao.collectionStore().dropIndex(index.get("name").toString());
    }

    public void recreateIndex() {
        Document index = this.findIndex();
        var isNew = true;
        if (index != null && !this.isUnique(index)) {
            this.dropIndex(index);
            isNew = false;
        }
        this.createRequiredUniqueIndex();
        log(isNew);
    }

    private void log(boolean isCreate) {
        var action = isCreate ? "create." : "recreate.";
        logger.debug("Init " + mongoDao.collectionStoreName() + ". Index " + action);
    }

}
