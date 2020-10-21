package com.iri.backend.tools.dao;

import com.iri.backend.tools.chengeSet.ChangeEntry;
import com.iri.backend.tools.service.IMongoDao;
import io.reactivex.Flowable;
import io.reactivex.Single;

public class ChangeEntryDao {

    private IMongoDao mongoDao;

    public ChangeEntryDao(IMongoDao mongoDao) {
        this.mongoDao = mongoDao;
    }

    public boolean isNewChange(ChangeEntry changeEntry) {
        return Flowable.fromPublisher(
                mongoDao.collectionStore().find(changeEntry.toSearchDocument())
        ).isEmpty().blockingGet();
    }

    public void save(ChangeEntry changeEntry) {
        Single.fromPublisher(
                mongoDao.collectionStore().insertOne(changeEntry.toFullDocument())
        ).blockingGet();
    }

}
