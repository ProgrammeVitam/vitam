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
package fr.gouv.vitam.common.database.server.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;
import fr.gouv.vitam.common.server.application.configuration.DbConfiguration;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MongoDbAccess interface
 */
public abstract class MongoDbAccess implements DatabaseConnection {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbAccess.class);

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private MongoDatabase mongoAdmin;
    private String dbname;

    /**
     * @param mongoClient MongoClient
     * @param dbname MongoDB database name
     * @param recreate True to recreate the index
     * @throws IllegalArgumentException if mongoClient or dbname is null
     */
    public MongoDbAccess(MongoClient mongoClient, final String dbname, final boolean recreate) {
        ParametersChecker.checkParameter("Parameter of MongoDbAccess", mongoClient, dbname);
        this.mongoClient = mongoClient;
        mongoDatabase = mongoClient.getDatabase(dbname);
        mongoAdmin = mongoClient.getDatabase("admin");
        this.dbname = dbname;
    }

    @Override
    public boolean checkConnection() {
        try {
            mongoClient.getDatabase(dbname).runCommand(new BasicDBObject("ping", "1"));
            return true;
        } catch (final Exception e) {
            LOGGER.warn(e);
            return false;
        }
    }

    /**
     * @return MongoClient
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * @param mongoClient MongoClient
     * @return MongoDbAccess
     */
    public MongoDbAccess setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        return this;
    }

    /**
     * @return MongoDatabase
     */
    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    /**
     * @param mongoDatabase MongoDatabase
     * @return MongoDbAccess
     */
    public MongoDbAccess setMongoDatabase(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
        return this;
    }

    /**
     * @return MongoDatabase
     */
    public MongoDatabase getMongoAdmin() {
        return mongoAdmin;
    }

    /**
     * @param mongoAdmin MongoDatabase
     * @return MongoDbAccess
     */
    public MongoDbAccess setMongoAdmin(MongoDatabase mongoAdmin) {
        this.mongoAdmin = mongoAdmin;
        return this;
    }

    /**
     * Close database access
     */
    public void close() {
        mongoClient.close();
    }

    @Override
    public String getInfo() {
        return dbname;
    }

    /**
     * Create a mongoDB client according to the configuration and using the MongoClientOptions specific to the
     * sub-systems (ex: metadata,logbook)
     *
     * @param configuration the configuration of mongo client (host/port to connect)
     * @param options the option mongo client
     * @return the MongoClient
     */
    public static MongoClient createMongoClient(DbConfiguration configuration, MongoClientOptions options) {
        final List<MongoDbNode> nodes = configuration.getMongoDbNodes();
        final List<ServerAddress> serverAddress = new ArrayList<>();
        for (final MongoDbNode node : nodes) {
            serverAddress.add(new ServerAddress(node.getDbHost(), node.getDbPort()));
        }

        if (configuration.isDbAuthentication()) {

            // create user with username, password and specify the database name
            final MongoCredential credential = MongoCredential.createCredential(
                configuration.getDbUserName(), configuration.getDbName(), configuration.getDbPassword().toCharArray());

            // create an instance of mongoclient
            return new MongoClient(serverAddress, Arrays.asList(credential), options);
        } else {
            return new MongoClient(serverAddress, options);
        }
    }

    /**
     * Change the target database
     *
     * @param dbname Name of the target database
     */
    public void setDatabase(String dbname) {
        mongoDatabase = mongoClient.getDatabase(dbname);
        this.dbname = dbname;
    }

}
