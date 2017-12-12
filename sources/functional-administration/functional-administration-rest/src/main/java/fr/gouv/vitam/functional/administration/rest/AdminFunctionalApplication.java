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
package fr.gouv.vitam.functional.administration.rest;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryFactory;
import fr.gouv.vitam.functional.administration.common.ReconstructionFactory;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.security.internal.filter.BasicAuthenticationFilter;

public class AdminFunctionalApplication extends Application {

    private Set<Object> singletons;

    public AdminFunctionalApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);
        AdminApplication adminApplication = new AdminApplication();

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final AdminManagementConfiguration
                configuration = PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);

            singletons = new HashSet<>();
            singletons.addAll(adminApplication.getSingletons());

            final AdminManagementResource resource = new AdminManagementResource(configuration);

            final MongoDbAccessAdminImpl mongoDbAccess = resource.getLogbookDbAccess();

            final ReconstructionFactory reconstructionFactory = VitamRepositoryFactory.getInstance();
            singletons.add(new ReconstructionResource(configuration, reconstructionFactory));

            Map<Integer, List<String>> externalIdentifiers = configuration.getListEnableExternalIdentifiers();
            final VitamCounterService vitamCounterService =
                new VitamCounterService(mongoDbAccess, configuration.getTenants(), externalIdentifiers);

            FunctionalBackupService functionalBackupService = new FunctionalBackupService(vitamCounterService);

            ContextResource contextResource = new ContextResource(mongoDbAccess, vitamCounterService,
                functionalBackupService);

            AdminContextResource adminContextResource = new AdminContextResource(contextResource);
            singletons.add(adminContextResource);

            SecurityProfileResource securityProfileResource =
                    new SecurityProfileResource(mongoDbAccess, vitamCounterService, functionalBackupService);
            AdminSecurityProfileResource adminSecurityProfileResource =
                    new AdminSecurityProfileResource(securityProfileResource);
            singletons.add(adminSecurityProfileResource);

            singletons.add(new BasicAuthenticationFilter(configuration));

        } catch (VitamException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
