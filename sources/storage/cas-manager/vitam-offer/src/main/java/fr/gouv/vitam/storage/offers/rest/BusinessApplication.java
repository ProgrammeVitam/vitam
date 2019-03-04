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
package fr.gouv.vitam.storage.offers.rest;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.storage.offers.core.DefaultOfferService;
import fr.gouv.vitam.storage.offers.core.DefaultOfferServiceImpl;
import fr.gouv.vitam.storage.offers.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.database.OfferSequenceDatabaseService;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogRepository;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogServiceImpl;
import fr.gouv.vitam.storage.offers.tape.rest.TapeCatalogResource;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;

/**
 * Offer register resources and filters
 */
public class BusinessApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;
    private Set<Object> singletons;

    /**
     * Constructor 
     * @param servletConfig the servlet configuration
     */
    public BusinessApplication(@Context ServletConfig servletConfig) {
        singletons = new HashSet<>();
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);
        commonBusinessApplication = new CommonBusinessApplication();

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {

            final OfferConfiguration configuration = PropertiesUtils.readYaml(yamlIS, OfferConfiguration.class);

            MongoClientOptions mongoClientOptions = VitamCollection.getMongoClientOptions();
            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration, mongoClientOptions);
            MongoDatabase database = mongoClient.getDatabase(configuration.getDbName());
            OfferSequenceDatabaseService offerSequenceDatabaseService = new OfferSequenceDatabaseService(database);

            OfferLogDatabaseService offerDatabaseService =
                new OfferLogDatabaseService(offerSequenceDatabaseService, database);

            DefaultOfferService defaultOfferService = new DefaultOfferServiceImpl(offerDatabaseService);
            DefaultOfferResource defaultOfferResource = new DefaultOfferResource(defaultOfferService);

            TapeCatalogRepository tapeCatalogRepository = new TapeCatalogRepository(new SimpleMongoDBAccess(mongoClient, configuration.getDbName()));
            TapeCatalogService tapeCatalogService = new TapeCatalogServiceImpl(tapeCatalogRepository);

            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(defaultOfferResource);
            singletons.add(new TapeCatalogResource(tapeCatalogService));

        } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return commonBusinessApplication.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
