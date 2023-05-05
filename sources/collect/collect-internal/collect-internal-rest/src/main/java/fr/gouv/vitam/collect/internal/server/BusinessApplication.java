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
package fr.gouv.vitam.collect.internal.server;

import com.fasterxml.jackson.jaxrs.base.JsonParseExceptionMapper;
import com.mongodb.client.MongoClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.configuration.CollectInternalConfiguration;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.collect.internal.core.repository.TransactionRepository;
import fr.gouv.vitam.collect.internal.core.service.CollectService;
import fr.gouv.vitam.collect.internal.core.service.FluxService;
import fr.gouv.vitam.collect.internal.core.service.MetadataService;
import fr.gouv.vitam.collect.internal.core.service.ProjectService;
import fr.gouv.vitam.collect.internal.core.service.SipService;
import fr.gouv.vitam.collect.internal.core.service.TransactionService;
import fr.gouv.vitam.collect.internal.rest.CollectMetadataInternalResource;
import fr.gouv.vitam.collect.internal.rest.ProjectInternalResource;
import fr.gouv.vitam.collect.internal.rest.TransactionInternalResource;
import fr.gouv.vitam.collect.internal.thread.ManageStatusThread;
import fr.gouv.vitam.collect.internal.thread.PurgeTransactionThread;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.serverv2.ConfigurationApplication;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetadataType;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * module declaring business resource
 */
public class BusinessApplication extends ConfigurationApplication {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BusinessApplication.class);
    private final Set<Object> singletons;

    /**
     * Constructor
     *
     * @param servletConfig servletConfig
     */
    public BusinessApplication(@Context ServletConfig servletConfig) throws CollectInternalException {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);
        singletons = new HashSet<>();
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final CollectInternalConfiguration
                configuration = PropertiesUtils.readYaml(yamlIS, CollectInternalConfiguration.class);
            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration);
            SimpleMongoDBAccess mongoDbAccess = new SimpleMongoDBAccess(mongoClient, configuration.getDbName());

            // Vitam Clients
            WorkspaceClientFactory.changeMode(configuration.getWorkspaceUrl(), WorkspaceType.COLLECT);
            MetaDataClientFactory metadataCollectClientFactory =
                MetaDataClientFactory.getInstance(MetadataType.COLLECT);
            WorkspaceClientFactory workspaceCollectClientFactory =
                WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
            IngestInternalClientFactory ingestInternalClientFactory = IngestInternalClientFactory.getInstance();
            AccessInternalClientFactory accessInternalClientFactory = AccessInternalClientFactory.getInstance();


            // Repositories
            TransactionRepository transactionRepository = new TransactionRepository(mongoDbAccess);
            ProjectRepository projectRepository = new ProjectRepository(mongoDbAccess);
            MetadataRepository metadataRepository = new MetadataRepository(metadataCollectClientFactory);

            // Services
            MetadataService metadataService = new MetadataService(metadataRepository, projectRepository);
            ProjectService projectService = new ProjectService(projectRepository);
            TransactionService transactionService =
                new TransactionService(transactionRepository, projectService, metadataRepository,
                    workspaceCollectClientFactory, accessInternalClientFactory, ingestInternalClientFactory);
            SipService sipService = new SipService(workspaceCollectClientFactory, metadataRepository);
            CollectService collectService =
                new CollectService(metadataRepository, workspaceCollectClientFactory,
                    FormatIdentifierFactory.getInstance());
            FluxService fluxService = new FluxService(collectService, metadataService, projectRepository,
                metadataRepository);


            // Resources
            final TransactionInternalResource transactionInternalResource =
                new TransactionInternalResource(transactionService, sipService,
                    metadataService, fluxService, projectService);
            final ProjectInternalResource projectInternalResource =
                new ProjectInternalResource(projectService, transactionService, metadataService);
            final CollectMetadataInternalResource collectMetadataInternalResource =
                new CollectMetadataInternalResource(metadataService,
                    collectService, transactionService);

            // Threads
            new PurgeTransactionThread(configuration, transactionService);
            new ManageStatusThread(configuration, transactionService);

            CommonBusinessApplication commonBusinessApplication = new CommonBusinessApplication();
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(new JsonParseExceptionMapper());
            singletons.add(transactionInternalResource);
            singletons.add(projectInternalResource);
            singletons.add(collectMetadataInternalResource);
        } catch (IOException e) {
            LOGGER.debug("Error when starting BusinessApplication : {}", e);
            throw new CollectInternalException(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
