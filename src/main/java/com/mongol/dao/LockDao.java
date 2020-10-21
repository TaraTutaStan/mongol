package com.iri.backend.tools.dao;


import com.iri.backend.tools.service.IMongoDao;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.IndexOptions;
import io.reactivex.Flowable;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mongodb.client.model.Filters.eq;


public class LockDao {
    private static final Logger logger = LoggerFactory.getLogger(LockDao.class);
    private static final String KEY_PROP_NAME = "key";
    private static final String INDEX_NAME = "mongollock_key_idx";

    private static final int INDEX_SORT_ASC = 1;

    private static final String LOCK_ENTRY_KEY_VAL = "LOCK";
    private final IndexOptions indexOptions;

    private final IMongoDao mongoDao;
    private final Document indexKeys;
    private final Document marker;

    public LockDao(IMongoDao mongoDao) {
        this.mongoDao = mongoDao;
        indexOptions = new IndexOptions().unique(true).name(INDEX_NAME);
        indexKeys = new Document(KEY_PROP_NAME, INDEX_SORT_ASC);
        marker = new Document(KEY_PROP_NAME, LOCK_ENTRY_KEY_VAL).append("status", "LOCK_HELD");

        mongoDao.collectionLock().createIndex(indexKeys, indexOptions);
    }

    public boolean acquireLock() {
        try {
            Flowable.fromPublisher(mongoDao.collectionLock().insertOne(marker))
                    .firstElement()
                    .blockingGet();
        } catch (MongoWriteException ex) {
            if (ex.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                logger.warn("Duplicate key exception.");
            }
            return false;
        }
        return true;
    }

    public void releaseLock() {
        Flowable.fromPublisher(
                mongoDao.collectionLock().findOneAndDelete(eq(KEY_PROP_NAME, LOCK_ENTRY_KEY_VAL))
        ).firstElement().blockingGet();
    }

    public boolean isLocked() {
        return Flowable.fromPublisher(
                mongoDao.collectionLock().count()
        ).firstElement().blockingGet() == 1;
    }

}
