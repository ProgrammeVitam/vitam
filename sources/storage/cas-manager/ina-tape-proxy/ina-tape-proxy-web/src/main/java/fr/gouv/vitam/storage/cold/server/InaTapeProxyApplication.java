/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.cold.server.rest.InaTapeProxyResource;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyExceptionMapper;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeCatalogRepository;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeDriveRepository;
import fr.gouv.vitam.storage.cold.server.simulator.service.InaService;
import fr.gouv.vitam.storage.cold.server.simulator.service.TapeInitializationService;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * JAX-RS Application for INA Tape Proxy
 */
public class InaTapeProxyApplication extends Application {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaTapeProxyApplication.class);

    private final CommonBusinessApplication commonBusinessApplication;
    private final Set<Object> singletons;

    public InaTapeProxyApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        InaTapeProxyConfiguration configuration = loadConfiguration(configurationFile);
        this.commonBusinessApplication = new CommonBusinessApplication();
        this.singletons = initResources(configuration);
    }

    private InaTapeProxyConfiguration loadConfiguration(String configurationFile) {
        try (InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            InaTapeProxyConfiguration config = PropertiesUtils.readYaml(yamlIS, InaTapeProxyConfiguration.class);
            LOGGER.info("Configuration loaded from: {}", configurationFile);
            return config;
        } catch (IOException e) {
            LOGGER.error("Unable to load configuration from: {}", configurationFile, e);
            throw new RuntimeException("Unable to load configuration", e);
        }
    }

    private Set<Object> initResources(InaTapeProxyConfiguration configuration) {
        try {
            Set<Object> set = new HashSet<>(commonBusinessApplication.getResources());

            // Initialize MongoDB connection
            LOGGER.info("Initializing MongoDB connection...");
            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration);
            MongoDatabase mongoDatabase = mongoClient.getDatabase(configuration.getDbName());
            LOGGER.info("MongoDB client created");

            // Initialize repositories
            TapeCatalogRepository tapeCatalogRepository = new TapeCatalogRepository(mongoDatabase);
            TapeDriveRepository tapeDriveRepository = new TapeDriveRepository(mongoDatabase);

            // Initialize services
            TapeInitializationService tapeInitializationService = new TapeInitializationService(
                configuration,
                tapeCatalogRepository,
                tapeDriveRepository
            );

            InaService service = tapeInitializationService.initializeService();

            set.add(new InaTapeProxyResource(service));
            set.add(new InaTapeProxyExceptionMapper());

            LOGGER.info("Resources initialized");
            return set;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize resources", e);
            throw new VitamRuntimeException("Failed to initialize resources", e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
