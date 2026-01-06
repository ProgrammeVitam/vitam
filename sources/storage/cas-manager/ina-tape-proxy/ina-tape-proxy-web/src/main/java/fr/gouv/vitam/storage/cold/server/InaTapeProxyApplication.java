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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.cold.server.simulator.InaIoService;
import fr.gouv.vitam.storage.cold.server.simulator.InaLibraryService;
import fr.gouv.vitam.storage.cold.server.simulator.InaService;
import fr.gouv.vitam.storage.cold.server.simulator.InaStatusService;
import fr.gouv.vitam.storage.cold.server.simulator.filesystem.FileSystemManager;
import fr.gouv.vitam.storage.cold.server.simulator.filesystem.MarkerManager;
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

            // Initialize services directly
            InaService service = initializeService(configuration);

            // Add our resource implementation with service
            set.add(new InaTapeProxyResourceImpl(service));

            LOGGER.info("Resources initialized");
            return set;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize resources", e);
            throw new VitamRuntimeException("Failed to initialize resources", e);
        }
    }

    private InaService initializeService(InaTapeProxyConfiguration configuration) throws IOException {
        // 1. Initialize filesystem manager
        LOGGER.info("Initializing FileSystemManager...");
        FileSystemManager fileSystemManager = new FileSystemManager(configuration);
        fileSystemManager.initialize();
        LOGGER.info("FileSystemManager initialized");

        // 2. Initialize marker manager
        LOGGER.info("Initializing MarkerManager...");
        MarkerManager markerManager = new MarkerManager(fileSystemManager);
        LOGGER.info("MarkerManager initialized");

        // 3. Initialize sub-services
        LOGGER.info("Initializing sub-services...");
        InaIoService ioService = new InaIoService(configuration, fileSystemManager, markerManager);
        LOGGER.info("  - InaIoService initialized");

        InaLibraryService roboticService = new InaLibraryService(configuration);
        LOGGER.info("  - InaLibraryService initialized");

        InaStatusService statusService = new InaStatusService(configuration);
        LOGGER.info("  - InaStatusService initialized");

        LOGGER.info("All sub-services initialized");

        // 4. Create main service
        LOGGER.info("Initializing main InaService...");
        InaService service = new InaService(configuration, ioService, roboticService, statusService);
        LOGGER.info("InaService initialized");

        return service;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
