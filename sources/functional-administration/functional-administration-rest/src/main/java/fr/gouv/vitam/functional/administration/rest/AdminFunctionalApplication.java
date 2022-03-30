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
package fr.gouv.vitam.functional.administration.rest;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.collections.CachedOntologyLoader;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.client.FunctionAdministrationOntologyLoader;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfigurationValidator;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.security.internal.filter.AdminRequestIdFilter;
import fr.gouv.vitam.security.internal.filter.BasicAuthenticationFilter;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * Admin functional Application declaring resources for the functional administration of Vitam
 */
public class AdminFunctionalApplication extends Application {

    private Set<Object> singletons;

    /**
     * Construcror
     *
     * @param servletConfig the configuration for the application
     */
    public AdminFunctionalApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);
        AdminApplication adminApplication = new AdminApplication();

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final AdminManagementConfiguration configuration =
                PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);

            // Validate configuration
            AdminManagementConfigurationValidator.validateConfiguration(configuration);

            // Elasticsearch configuration
            ElasticsearchFunctionalAdminIndexManager indexManager =
                new ElasticsearchFunctionalAdminIndexManager(configuration);

            singletons = new HashSet<>();
            singletons.addAll(adminApplication.getSingletons());

            CachedOntologyLoader ontologyLoader = new CachedOntologyLoader(
                VitamConfiguration.getOntologyCacheMaxEntries(),
                VitamConfiguration.getOntologyCacheTimeoutInSeconds(),
                new FunctionAdministrationOntologyLoader()
            );

            final AdminManagementResource resource = new AdminManagementResource(configuration, ontologyLoader,
                indexManager);

            final MongoDbAccessAdminImpl mongoDbAccess = resource.getLogbookDbAccess();

            final VitamRepositoryProvider vitamRepositoryProvider = VitamRepositoryFactory.get();
            singletons.add(new AdminReconstructionResource(configuration, vitamRepositoryProvider, ontologyLoader,
                indexManager));
            singletons.add(new ReindexationResource(indexManager));

            Map<Integer, List<String>> externalIdentifiers = configuration.getListEnableExternalIdentifiers();
            final VitamCounterService vitamCounterService =
                new VitamCounterService(mongoDbAccess, VitamConfiguration.getTenants(), externalIdentifiers);

            FunctionalBackupService functionalBackupService = new FunctionalBackupService(vitamCounterService);

            AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient();

            ContextResource contextResource = new ContextResource(mongoDbAccess, vitamCounterService,
                functionalBackupService, adminManagementClient);

            AdminContextResource adminContextResource = new AdminContextResource(contextResource);
            singletons.add(adminContextResource);

            OntologyResource ontologyResource = new OntologyResource(mongoDbAccess, functionalBackupService);
            AdminOntologyResource adminOntologyResource =
                new AdminOntologyResource(ontologyResource, mongoDbAccess, functionalBackupService);
            singletons.add(adminOntologyResource);

            SecurityProfileResource securityProfileResource =
                new SecurityProfileResource(mongoDbAccess, vitamCounterService, functionalBackupService,
                    adminManagementClient);
            AdminSecurityProfileResource adminSecurityProfileResource =
                new AdminSecurityProfileResource(securityProfileResource);
            singletons.add(adminSecurityProfileResource);

            AdminDataMigrationResource adminDataMigrationResource = new AdminDataMigrationResource();
            singletons.add(adminDataMigrationResource);
            singletons.add(new AdminMigrationResource(adminDataMigrationResource));
            singletons.add(new AdminOperationResource());

            singletons.add(new BasicAuthenticationFilter(configuration));
            singletons.add(new AdminRequestIdFilter());

        } catch (VitamException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
