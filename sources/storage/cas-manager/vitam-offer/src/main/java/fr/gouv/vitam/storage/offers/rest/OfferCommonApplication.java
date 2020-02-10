/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.offers.rest;

import com.google.common.base.Strings;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.storage.offers.core.DefaultOfferService;
import fr.gouv.vitam.storage.offers.core.DefaultOfferServiceImpl;
import fr.gouv.vitam.storage.offers.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferSequenceDatabaseService;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class OfferCommonApplication {

    private static OfferCommonApplication instance = new OfferCommonApplication();

    public static OfferCommonApplication getInstance() {
        return instance;
    }

    private MongoDbAccess mongoDbAccess;
    private StorageConfiguration storageConfiguration;

    private DefaultOfferService defaultOfferService;

    synchronized void initialize(String configurationFile) {
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {

            final OfferConfiguration configuration = PropertiesUtils.readYaml(yamlIS, OfferConfiguration.class);

            MongoClientOptions mongoClientOptions = VitamCollection.getMongoClientOptions();
            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration, mongoClientOptions);
            MongoDatabase mongoDatabase = mongoClient.getDatabase(configuration.getDbName());
            OfferSequenceDatabaseService offerSequenceDatabaseService = new OfferSequenceDatabaseService(mongoDatabase);

            OfferLogDatabaseService offerDatabaseService = new OfferLogDatabaseService(offerSequenceDatabaseService, mongoDatabase);

            mongoDbAccess = new SimpleMongoDBAccess(mongoClient, configuration.getDbName());

            storageConfiguration = PropertiesUtils
                .readYaml(PropertiesUtils.findFile(DefaultOfferService.STORAGE_CONF_FILE_NAME),
                    StorageConfiguration.class);
            if (!Strings.isNullOrEmpty(storageConfiguration.getStoragePath())) {
                storageConfiguration
                    .setStoragePath(FileUtil.getFileCanonicalPath(storageConfiguration.getStoragePath()));
            }

            defaultOfferService = new DefaultOfferServiceImpl(offerDatabaseService, mongoDbAccess);
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public synchronized void reset() {
        this.defaultOfferService = null;
    }

    DefaultOfferService getDefaultOfferService() {
        return defaultOfferService;
    }

    public MongoDbAccess getMongoDbAccess() {
        return mongoDbAccess;
    }

    StorageConfiguration getStorageConfiguration() {
        return storageConfiguration;
    }


}
