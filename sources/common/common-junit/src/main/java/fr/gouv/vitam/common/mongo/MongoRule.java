/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.mongo;

import com.google.common.collect.Sets;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.junit.rules.ExternalResource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Launch a single instance of Mongo database, drop collection after each test
 */
public class MongoRule extends ExternalResource {

    public static final String VITAM_DB = "vitam-test";
    public static final String MONGO_HOST = "localhost";
    public static final String VITAM_SEQUENCE = "VitamSequence";
    public static final String COUNTER = "Counter";
    public static final String ID = "_id";

    private static final int dataBasePort = 27017;

    private final MongoClient mongoClient;
    private final String dbName;
    private final Set<String> collectionsToBePurged;

    private boolean clientClosed = false;

    public MongoRule(MongoClientSettings.Builder mongoClientSettingsBuilder, String... collectionsToBePurged) {
        this(VITAM_DB, mongoClientSettingsBuilder, collectionsToBePurged);
    }

    public MongoRule(
        String dbName,
        MongoClientSettings.Builder mongoClientSettingsBuilder,
        String... collectionsToBePurged
    ) {
        if (null != collectionsToBePurged) {
            this.collectionsToBePurged = Sets.newHashSet(collectionsToBePurged);
        } else {
            this.collectionsToBePurged = new HashSet<>();
        }

        mongoClientSettingsBuilder.applyToClusterSettings(
            builder -> builder.hosts(List.of(new ServerAddress(MONGO_HOST, dataBasePort)))
        );

        this.mongoClient = MongoClients.create(mongoClientSettingsBuilder.build());
        this.dbName = dbName;
    }

    public static String getDatabaseName() {
        return VITAM_DB;
    }

    @Override
    protected void after() {
        if (!clientClosed) {
            purge(dbName, collectionsToBePurged);
        }
    }

    private void purge(String database, Collection<String> collectionsToBePurged) {
        for (String collectionName : collectionsToBePurged) {
            if (VITAM_SEQUENCE.equals(collectionName)) {
                mongoClient
                    .getDatabase(database)
                    .getCollection(collectionName)
                    .updateMany(Filters.exists(ID), Updates.set(COUNTER, 0));
                continue;
            }
            mongoClient.getDatabase(database).getCollection(collectionName).deleteMany(new Document());
        }
    }

    // Add index to be purged
    public MongoRule addCollectionToBePurged(String collectionName) {
        collectionsToBePurged.add(collectionName);
        return this;
    }

    /**
     * Used when annotated @ClassRule
     */
    public void handleAfterClass() {
        after();
        close();
    }

    public void handleAfter() {
        after();
    }

    public static int getDataBasePort() {
        return dataBasePort;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoClient.getDatabase(dbName);
    }

    public MongoCollection<Document> getMongoCollection(String collectionName) {
        return mongoClient.getDatabase(dbName).getCollection(collectionName);
    }

    public <TDocument> MongoCollection<TDocument> getMongoCollection(String collectionName, Class<TDocument> clazz) {
        return mongoClient.getDatabase(dbName).getCollection(collectionName, clazz);
    }

    public void handleAfter(Set<String> collections) {
        purge(dbName, collections);
    }

    public void close() {
        mongoClient.close();
        clientClosed = true;
    }
}
