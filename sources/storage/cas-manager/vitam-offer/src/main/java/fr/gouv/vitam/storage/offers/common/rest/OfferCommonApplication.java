/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.common.rest;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferService;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferServiceImpl;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.database.OfferSequenceDatabaseService;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class OfferCommonApplication {

    private static OfferCommonApplication INSTANCE = new OfferCommonApplication();
    public static OfferCommonApplication getInstance() {
        return INSTANCE;
    }

    private MongoDatabase mongoDatabase;
    private DefaultOfferService defaultOfferService;

    public synchronized void initialize(String configurationFile) {

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {

            final OfferConfiguration configuration = PropertiesUtils.readYaml(yamlIS, OfferConfiguration.class);

            MongoClientOptions mongoClientOptions = VitamCollection.getMongoClientOptions();
            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration, mongoClientOptions);
            mongoDatabase = mongoClient.getDatabase(configuration.getDbName());
            OfferSequenceDatabaseService offerSequenceDatabaseService = new OfferSequenceDatabaseService(mongoDatabase);

            OfferLogDatabaseService offerDatabaseService =
                new OfferLogDatabaseService(offerSequenceDatabaseService, mongoDatabase);

            defaultOfferService = new DefaultOfferServiceImpl(offerDatabaseService);

        } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void reset() {
        this.defaultOfferService = null;
    }

    public DefaultOfferService getDefaultOfferService() {
        return defaultOfferService;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public void setMongoDatabase(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }
}
