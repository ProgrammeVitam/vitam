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
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Launch a single instance of Mongo database, drop collection after each test
 */
public class MongoRule extends ExternalResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoRule.class);

    private static int dataBasePort;

    private static MongodExecutable mongodExecutable;

    static {
        dataBasePort = JunitHelper.getInstance().findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        try {
            mongodExecutable = starter.prepare(new MongodConfigBuilder()
                .withLaunchArgument("--enableMajorityReadConcern")
                .version(Version.Main.PRODUCTION)
                .net(new Net(dataBasePort, Network.localhostIsIPv6()))
                .build());
            mongodExecutable.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final MongoClient mongoClient;
    private String dataBaseName;
    private Set<String> collectionNames;

    private boolean clientClosed = false;

    /**
     * @param clientOptions
     * @param dataBaseName
     * @param collectionNames
     */
    public MongoRule(MongoClientOptions clientOptions, String dataBaseName, String... collectionNames) {
        this.dataBaseName = dataBaseName;
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
            purge(collectionNames);
        }

        printSystemInfo();
    }

    private void purge(Collection<String> collectionNames) {
        for (String collectionName : collectionNames) {
            if ("VitamSequence".equals(collectionName)) {
                mongoClient.getDatabase(dataBaseName).getCollection(collectionName).updateMany(Filters.exists("_id"),
                    Updates.set("Counter", 0));
            }
            mongoClient.getDatabase(dataBaseName).getCollection(collectionName).deleteMany(new Document());
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

    private void printSystemInfo() {
        long id = System.currentTimeMillis();
        LOGGER.error(id + "- Available processors (cores): " + Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        LOGGER.error(id + "- Free memory (bytes): " + Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        LOGGER.error(id + "- Maximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently available to the JVM */
        LOGGER.error(id + "- Total memory available to JVM (bytes): " + Runtime.getRuntime().totalMemory());

        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            LOGGER.error(id + "- File system root: " + root.getAbsolutePath());
            LOGGER.error(id + "- Total space (bytes): " + root.getTotalSpace());
            LOGGER.error(id + "- Free space (bytes): " + root.getFreeSpace());
            LOGGER.error(id + "- Usable space (bytes): " + root.getUsableSpace());
        }
    }

    public void handleAfter() {
        after();
    }

    public void stop() {
        mongodExecutable.stop();
    }

    public void start() {
        try {
            mongodExecutable.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getDataBasePort() {
        return dataBasePort;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoClient.getDatabase(dataBaseName);
    }

    public MongoCollection<Document> getMongoCollection(String collectionName) {
        return mongoClient.getDatabase(dataBaseName).getCollection(collectionName);
    }

    public <TDocument> MongoCollection<TDocument> getMongoCollection(String collectionName, Class<TDocument> clazz) {
        return mongoClient.getDatabase(dataBaseName).getCollection(collectionName, clazz);
    }

    public void handleAfter(Set<String> collections) {
        after(collections);
    }

    private void after(Set<String> collections) {
        purge(collections);
    }

    public void close() {
        mongoClient.close();
        clientClosed = true;
    }
}
