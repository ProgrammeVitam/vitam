/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.junit.rules.ExternalResource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Launch a single instance of Mongo database, drop collection after each test
 */
public class MongoRule extends ExternalResource {
    public static final String VITAM_DB = "vitam-test";

    private static int dataBasePort = 27017;

    private final MongoClient mongoClient;
    private Set<String> collectionNames;

    private boolean clientClosed = false;

    /**
     * @param clientOptions
     * @param collectionNames
     */
    public MongoRule(MongoClientOptions clientOptions, String... collectionNames) {

        if (null != collectionNames) {
            this.collectionNames = Sets.newHashSet(collectionNames);
        } else {
            this.collectionNames = new HashSet<>();
        }

        mongoClient = new MongoClient(new ServerAddress("localhost", dataBasePort), clientOptions);
    }

    @Override
    protected void after() {
        if (!clientClosed) {
            purge(MongoRule.VITAM_DB, collectionNames);
        }
    }

    private void purge(String database, Collection<String> collectionNames) {
        for (String collectionName : collectionNames) {
            if ("VitamSequence".equals(collectionName)) {
                mongoClient.getDatabase(database).getCollection(collectionName).updateMany(Filters.exists("_id"),
                    Updates.set("Counter", 0));
            }
            mongoClient.getDatabase(database).getCollection(collectionName).deleteMany(new Document());
        }
    }

    // Add index to be purged
    public MongoRule addCollectionToBePurged(String collection) {
        collectionNames.add(collection);
        return this;
    }

    /**
     * Used when annotated @ClassRule
     */
    public void handleAfterClass() {
        after();
        close();
    }

    public void handleAfterClass(String database) {
        handleAfter(database);
        handleAfterClass();
    }

    public void handleAfter() {
        after();
    }

    public void handleAfter(String database) {
        purge(database, collectionNames);
        handleAfter();
    }

    public static int getDataBasePort() {
        return dataBasePort;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoClient.getDatabase(VITAM_DB);
    }

    public MongoCollection<Document> getMongoCollection(String collectionName) {
        return mongoClient.getDatabase(VITAM_DB).getCollection(collectionName);
    }

    public <TDocument> MongoCollection<TDocument> getMongoCollection(String collectionName, Class<TDocument> clazz) {
        return mongoClient.getDatabase(VITAM_DB).getCollection(collectionName, clazz);
    }

    public void handleAfter(Set<String> collections) {
        purge(MongoRule.VITAM_DB, collections);
    }


    public void close() {
        mongoClient.close();
        clientClosed = true;
    }
}
