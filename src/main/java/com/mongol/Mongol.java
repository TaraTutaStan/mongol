package com.iri.backend.tools;


import com.iri.backend.common.codec.EnumCodecProvider;
import com.iri.backend.tools.chengeSet.ChangeEntry;
import com.iri.backend.tools.dao.ChangeEntryDao;
import com.iri.backend.tools.dao.IndexDao;
import com.iri.backend.tools.dao.LockDao;
import com.iri.backend.tools.exception.MongolBaseException;
import com.iri.backend.tools.exception.MongolConfigurationException;
import com.iri.backend.tools.service.ChangeLogService;
import com.iri.backend.tools.service.IMongoDao;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class Mongol {

    private static final Logger logger = LoggerFactory.getLogger(Mongol.class);

    private static final String DEFAULT_COLLECTION_NAME = "databaseChangeLog";
    private static final String DEFAULT_LOCK_COLLECTION_NAME = "databaseChangeLogLock";

    private ChangeEntryDao dao;

    private boolean enabled = true;
    private String changeLogsScanPackage;
    private MongoClient mongoClient;
    private Map<Class, Object> properties;
    private String dbName;
    private IMongoDao mongoDao;
    private IndexDao indexDao;
    private LockDao lockDao;

    public Mongol() {
    }

    public void execute() throws MongolBaseException {
        if (!isEnabled()) {
            logger.info("Migration is disabled. Exiting.");
            return;
        }

        validate();
        indexDao.recreateIndex();

        if (!lockDao.acquireLock()) {
            logger.info("Did not acquire process lock. Exiting.");
            return;
        }
        logger.info("Acquired process lock, starting the data migration sequence..");
        try {
            process();
        } finally {
            logger.info("Migration is releasing process lock.");
            lockDao.releaseLock();
        }

        logger.info("Migration has finished his job.");
    }

    private void process() throws MongolBaseException {

        ChangeLogService service = new ChangeLogService(changeLogsScanPackage);

        for (Class<?> changelogClass : service.getChangeLogs()) {

            Object changelogInstance = null;
            try {
                changelogInstance = changelogClass.getConstructor().newInstance();
                List<Method> changeSetMethods = service.getChangeSets(changelogInstance.getClass());

                for (Method changeSetMethod : changeSetMethods) {
                    ChangeEntry changeEntry = service.extractChangeEntry(changeSetMethod);

                    if (dao.isNewChange(changeEntry)) {
                        executeMethod(changeSetMethod, changelogInstance);
                        dao.save(changeEntry);
                        logger.info(changeEntry + " applied");
                    } else if (service.isRunAlways(changeSetMethod)) {
                        executeMethod(changeSetMethod, changelogInstance);
                        logger.info(changeEntry + " reapplied");
                    } else {
                        logger.info(changeEntry + " passed over");
                    }

                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                throw new MongolBaseException(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getTargetException();
                throw new MongolBaseException(targetException.getMessage(), e);
            }

        }
    }

    private Object executeMethod(Method changeSetMethod, Object changeLogInstance)
            throws IllegalAccessException, InvocationTargetException, MongolConfigurationException {
        if (changeSetMethod.getParameterTypes().length != 0) {
            final var parameter = extractParameter(changeSetMethod);
            return changeSetMethod.invoke(changeLogInstance, parameter.toArray());
        } else {
            logger.debug("method with no params");
            return changeSetMethod.invoke(changeLogInstance);
        }
    }

    private List<Object> extractParameter(Method changeSetMethod) throws MongolConfigurationException {
        if (changeSetMethod == null) {
            throw new MongolConfigurationException("Invoke interrupt. Method was null!");
        }

        if (changeSetMethod.getParameterTypes().length != 1) {
            throw new MongolConfigurationException("Invoke interrupt. Parameter was null!");
        }

        final var parameters = Arrays.stream(changeSetMethod.getParameterTypes())
                .peek(it -> logger.debug("Defined method with parameter", it))
                .map(this.properties::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (parameters.size() != changeSetMethod.getParameterTypes().length) {
            throw new MongolConfigurationException("Couldn't found few parameters in method " + changeSetMethod.getName());
        }
        return parameters;
    }

    private void validate() throws MongolConfigurationException {
        if (StringUtils.isBlank(dbName)) {
            throw new MongolConfigurationException("Validate interrupt.Property databaseName wasn't present");
        }

        if (StringUtils.isBlank(changeLogsScanPackage)) {
            throw new MongolConfigurationException("Validate interrupt.Property currentPackage wasn't present");
        }

        if (mongoClient == null) {
            throw new MongolConfigurationException("Validate interrupt. Property mongoClient wasn't present");
        }
    }

    public boolean isExecutionInProgress() {
        return lockDao.isLocked();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static Mongol.BuilderMigration builder() {
        return new Mongol.BuilderMigration();
    }

    public static class BuilderMigration {
        private String databaseName;
        private String currentPackage;
        private String storeCollection;
        private String lockCollection;
        private boolean enabled;
        private MongoClient mongoClient;
        private Map<Class, Object> properties;

        public Map<Class, Object> getProperties() {
            if (properties == null) {
                properties = new HashMap<>();
            }
            return properties;
        }

        BuilderMigration() {
        }

        public Mongol.BuilderMigration databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Mongol.BuilderMigration addProperties(@NotNull Class clazz, @NotNull Object value) {
            this.getProperties().put(clazz, value);
            return this;
        }

        public Mongol.BuilderMigration mongoClient(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
            return this;
        }

        public Mongol.BuilderMigration enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Mongol.BuilderMigration currentPackage(String currentPackage) {
            this.currentPackage = currentPackage;
            return this;
        }

        public Mongol.BuilderMigration storeCollection(String storeCollection) {
            this.storeCollection = storeCollection;
            return this;
        }

        public Mongol.BuilderMigration lockCollection(String lockCollection) {
            this.lockCollection = lockCollection;
            return this;
        }

        public Mongol build() {
            var result = new Mongol();

            result.properties = properties;
            result.dbName = databaseName;
            result.changeLogsScanPackage = currentPackage;
            result.mongoClient = mongoClient;
            result.enabled = enabled;

            final var store = StringUtils.isBlank(storeCollection) ?
                    DEFAULT_COLLECTION_NAME : storeCollection;

            final var lock = StringUtils.isBlank(lockCollection) ?
                    DEFAULT_LOCK_COLLECTION_NAME : lockCollection;

            result.mongoDao = new IMongoDao() {
                @Override
                public MongoDatabase db() {
                    return mongoClient.getDatabase(databaseName);
                }

                @Override
                public MongoClient client() {
                    return mongoClient;
                }

                @Override
                public MongoCollection<Document> collectionStore() {
                    return mongoClient.getDatabase(databaseName).getCollection(store)
                            .withCodecRegistry(codecRegistries());
                }

                @Override
                public MongoCollection<Document> collectionLock() {
                    return mongoClient.getDatabase(databaseName).getCollection(lock)
                            .withCodecRegistry(codecRegistries());
                }

                @Override
                public String collectionLockName() {
                    return lock;
                }

                @Override
                public String collectionStoreName() {
                    return store;
                }
            };

            result.indexDao = new IndexDao(result.mongoDao);
            result.lockDao = new LockDao(result.mongoDao);
            result.dao = new ChangeEntryDao(result.mongoDao);
            return result;
        }
    }

    protected static CodecRegistry codecRegistries() {
        List<CodecRegistry> codecRegistries = new ArrayList<>(
                List.of(
                        com.mongodb.MongoClient.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(
                                new EnumCodecProvider(),
                                PojoCodecProvider.builder().automatic(true).build()
                        )
                ));

        return CodecRegistries.fromRegistries(
                codecRegistries
        );
    }


}
