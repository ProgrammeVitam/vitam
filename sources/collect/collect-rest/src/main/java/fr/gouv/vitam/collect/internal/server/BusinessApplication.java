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
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.repository.ProjectRepository;
import fr.gouv.vitam.collect.internal.repository.TransactionRepository;
import fr.gouv.vitam.collect.internal.resource.TransactionResource;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.FluxService;
import fr.gouv.vitam.collect.internal.service.ProjectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.SecureEndpointScanner;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.serverv2.ConfigurationApplication;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.security.internal.filter.AuthorizationFilter;
import fr.gouv.vitam.security.internal.filter.InternalSecurityFilter;

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
    public BusinessApplication(@Context ServletConfig servletConfig) throws CollectException {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);
        singletons = new HashSet<>();
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final CollectConfiguration configuration =
                PropertiesUtils.readYaml(yamlIS, CollectConfiguration.class);
            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration);
            SimpleMongoDBAccess mongoDbAccess = new SimpleMongoDBAccess(mongoClient, configuration.getDbName());

            SecureEndpointRegistry secureEndpointRegistry = new SecureEndpointRegistry();
            SecureEndpointScanner secureEndpointScanner = new SecureEndpointScanner(secureEndpointRegistry);

            TransactionRepository transactionRepository = new TransactionRepository(mongoDbAccess);

            ProjectRepository projectRepository = new ProjectRepository(mongoDbAccess);
            ProjectService projectService = new ProjectService(projectRepository);
            TransactionService transactionService = new TransactionService(transactionRepository, projectService);
            SipService sipService = new SipService(configuration);
            CollectService collectService = new CollectService(transactionService, configuration);
            FluxService fluxService =
                new FluxService(collectService, configuration);
            CommonBusinessApplication commonBusinessApplication = new CommonBusinessApplication();

            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(new SanityCheckerCommonFilter());
            singletons.add(new InternalSecurityFilter());
            singletons.add(new AuthorizationFilter());
            singletons.add(new JsonParseExceptionMapper());
            singletons.add(
                new TransactionResource(secureEndpointRegistry, transactionService, collectService, sipService, projectService, fluxService));
            singletons.add(secureEndpointScanner);
        } catch (IOException e) {
            LOGGER.debug("Error when starting BusinessApplication : {}", e);
            throw new CollectException(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
