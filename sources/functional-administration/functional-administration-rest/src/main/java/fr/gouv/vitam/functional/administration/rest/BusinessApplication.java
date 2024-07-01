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
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementOntologyLoader;
import fr.gouv.vitam.functional.administration.common.client.FunctionAdministrationOntologyLoader;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfigurationValidator;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.agencies.AgenciesService;
import fr.gouv.vitam.functional.administration.core.archiveunitprofiles.ArchiveUnitProfileServiceImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.context.ContextService;
import fr.gouv.vitam.functional.administration.core.context.ContextServiceImpl;
import fr.gouv.vitam.functional.administration.core.griffin.GriffinService;
import fr.gouv.vitam.functional.administration.core.griffin.PreservationScenarioService;
import fr.gouv.vitam.functional.administration.core.ontologies.OntologyServiceImpl;
import fr.gouv.vitam.functional.administration.core.security.profile.SecurityProfileService;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.RULES;

/**
 * Business application for function administration declaring resources and filters
 */
public class BusinessApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;
    private final Set<Object> singletons;

    /**
     * Constructor
     *
     * @param servletConfig the servlet configuration
     */
    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);
        commonBusinessApplication = new CommonBusinessApplication();

        singletons = new HashSet<>();
        singletons.addAll(commonBusinessApplication.getResources());

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final AdminManagementConfiguration configuration = PropertiesUtils.readYaml(
                yamlIS,
                AdminManagementConfiguration.class
            );

            // Validate configuration
            AdminManagementConfigurationValidator.validateConfiguration(configuration);

            // Elasticsearch configuration
            ElasticsearchFunctionalAdminIndexManager indexManager = new ElasticsearchFunctionalAdminIndexManager(
                configuration
            );

            CachedOntologyLoader ontologyLoader = new CachedOntologyLoader(
                VitamConfiguration.getOntologyCacheMaxEntries(),
                VitamConfiguration.getOntologyCacheTimeoutInSeconds(),
                new FunctionAdministrationOntologyLoader()
            );
            CachedOntologyLoader agenciesOntologyLoader = new CachedOntologyLoader(
                VitamConfiguration.getOntologyCacheMaxEntries(),
                VitamConfiguration.getOntologyCacheTimeoutInSeconds(),
                new AdminManagementOntologyLoader(
                    AdminManagementClientFactory.getInstance(),
                    Optional.of(FunctionalAdminCollections.AGENCIES.getName())
                )
            );

            CachedOntologyLoader rulesOntologyLoader = new CachedOntologyLoader(
                VitamConfiguration.getOntologyCacheMaxEntries(),
                VitamConfiguration.getOntologyCacheTimeoutInSeconds(),
                new AdminManagementOntologyLoader(
                    AdminManagementClientFactory.getInstance(),
                    Optional.of(RULES.getName())
                )
            );

            final AdminManagementResource resource = new AdminManagementResource(
                configuration,
                ontologyLoader,
                indexManager
            );

            final MongoDbAccessAdminImpl mongoDbAccess = resource.getLogbookDbAccess();
            Map<Integer, List<String>> externalIdentifiers = configuration.getListEnableExternalIdentifiers();
            final VitamCounterService vitamCounterService = new VitamCounterService(
                mongoDbAccess,
                VitamConfiguration.getTenants(),
                externalIdentifiers
            );
            resource.setVitamCounterService(vitamCounterService);
            FunctionalBackupService functionalBackupService = new FunctionalBackupService(vitamCounterService);

            final SecurityProfileService securityProfileService = new SecurityProfileService(
                mongoDbAccess,
                vitamCounterService,
                functionalBackupService
            );

            final ContextService contextService = new ContextServiceImpl(mongoDbAccess, vitamCounterService);

            securityProfileService.setContextService(contextService);
            contextService.setSecurityProfileService(securityProfileService);

            final AgenciesService agenciesService = new AgenciesService(
                mongoDbAccess,
                vitamCounterService,
                functionalBackupService
            );
            final OntologyServiceImpl ontologyService = new OntologyServiceImpl(mongoDbAccess, functionalBackupService);
            final ArchiveUnitProfileServiceImpl archiveUnitProfileService = new ArchiveUnitProfileServiceImpl(
                mongoDbAccess,
                vitamCounterService,
                functionalBackupService
            );

            final VitamRepositoryProvider vitamRepositoryProvider = VitamRepositoryFactory.get();

            singletons.add(resource);
            singletons.add(new ArchiveUnitProfileResource(archiveUnitProfileService));
            singletons.add(new OntologyResource(ontologyService));
            singletons.add(new ContractResource(mongoDbAccess, vitamCounterService));
            singletons.add(new ContextResource(contextService));
            singletons.add(new SecurityProfileResource(securityProfileService));

            singletons.add(new AgenciesResource(agenciesService));
            singletons.add(new ReindexationResource(indexManager));
            singletons.add(new EvidenceResource(mongoDbAccess, vitamCounterService));
            singletons.add(
                new ReconstructionResource(configuration, vitamRepositoryProvider, ontologyLoader, indexManager)
            );
            singletons.add(new ProbativeValueResource());
            singletons.add(
                new ProfileResource(configuration, mongoDbAccess, vitamCounterService, functionalBackupService)
            );

            PreservationScenarioService preservationScenarioService = new PreservationScenarioService(
                mongoDbAccess,
                functionalBackupService
            );

            GriffinService griffinService = new GriffinService(mongoDbAccess, functionalBackupService);
            PreservationResource griffinResource = new PreservationResource(
                preservationScenarioService,
                griffinService
            );

            singletons.add(griffinResource);
        } catch (IOException | VitamException e) {
            throw new VitamRuntimeException(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
