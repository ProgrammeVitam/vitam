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
package fr.gouv.vitam.logbook.rest;

import com.google.common.base.Throwables;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.collections.CachedOntologyLoader;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementOntologyLoader;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfigurationValidator;
import fr.gouv.vitam.security.internal.filter.AdminRequestIdFilter;
import fr.gouv.vitam.security.internal.filter.BasicAuthenticationFilter;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * Admin application.
 */
public class AdminLogbookApplication extends Application {

    private final AdminApplication adminApplication;

    private Set<Object> singletons;

    /**
     * Constructor
     *
     * @param servletConfig servletConfig
     */
    public AdminLogbookApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final LogbookConfiguration logbookConfiguration = PropertiesUtils.readYaml(
                yamlIS,
                LogbookConfiguration.class
            );

            // Validate configuration
            LogbookConfigurationValidator.validateConfiguration(logbookConfiguration);

            // Elasticsearch configuration
            ElasticsearchLogbookIndexManager indexManager = new ElasticsearchLogbookIndexManager(
                logbookConfiguration,
                VitamConfiguration.getTenants()
            );

            adminApplication = new AdminApplication();
            CachedOntologyLoader ontologyLoader = new CachedOntologyLoader(
                VitamConfiguration.getOntologyCacheMaxEntries(),
                VitamConfiguration.getOntologyCacheTimeoutInSeconds(),
                new AdminManagementOntologyLoader(AdminManagementClientFactory.getInstance(), Optional.empty())
            );

            VitamRepositoryProvider vitamRepositoryProvider = VitamRepositoryFactory.get();

            singletons = new HashSet<>();
            singletons.addAll(adminApplication.getSingletons());
            singletons.add(new LogbookAdminResource(vitamRepositoryProvider, logbookConfiguration));
            singletons.add(new BasicAuthenticationFilter(logbookConfiguration));
            singletons.add(new AdminRequestIdFilter());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
