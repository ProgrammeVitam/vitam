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

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.junit.JunitHelper;
import org.bson.Document;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Launch a single instance of Mongo database, drop collection after each test
 */
public class MongoRule extends ExternalResource {

    private static int dataBasePort;

    static {
        dataBasePort = JunitHelper.getInstance().findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        try {
            MongodExecutable mongodExecutable = starter.prepare(new MongodConfigBuilder()
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
    private String clusterName;
    private List<String> collectionNames;

    public MongoRule(MongoClientOptions clientOptions, String clusterName, String... collectionNames) {
        this.clusterName = clusterName;
        this.collectionNames = Arrays.asList(collectionNames);

        mongoClient = new MongoClient(new ServerAddress("localhost", dataBasePort), clientOptions);
    }

    @Override
    protected void after() {
        for (String collectionName : collectionNames) {
            mongoClient.getDatabase(clusterName).getCollection(collectionName).drop();
        }
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoClient.getDatabase(clusterName);
    }

    public MongoCollection<Document> getMongoCollection(String collectionName) {
        return mongoClient.getDatabase(clusterName).getCollection(collectionName);
    }

}
